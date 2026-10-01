package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.TipoNotificacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    String FETCH_DETALLE = "select c from Cita c "
            + "join fetch c.paciente p join fetch p.usuario pu "
            + "join fetch c.horario h join fetch h.medico m join fetch m.usuario mu "
            + "join fetch m.especialidad e join fetch h.consultorio co ";

    @Query(FETCH_DETALLE + "where c.id = :id")
    Optional<Cita> findDetalleById(@Param("id") Long id);

    @Query(FETCH_DETALLE + "where p.id = :pacienteId order by h.fecha desc, h.horaInicio desc")
    List<Cita> findByPaciente(@Param("pacienteId") Long pacienteId);

    /** Agenda de un día (todas las citas). Se filtra por médico / especialidad en el servicio. */
    @Query(FETCH_DETALLE + "where h.fecha = :fecha order by h.horaInicio, mu.apellidos")
    List<Cita> findAgendaPorFecha(@Param("fecha") LocalDate fecha);

    /** RF-35: ¿existe una cita activa del mismo paciente, especialidad, fecha y hora? */
    @Query("select count(c) > 0 from Cita c join c.horario h join h.medico m "
            + "where c.paciente.id = :pacienteId and m.especialidad.id = :especialidadId "
            + "and h.fecha = :fecha and h.horaInicio = :hora and c.estado in :estados and c.id <> :excluirId")
    boolean existeDuplicada(@Param("pacienteId") Long pacienteId, @Param("especialidadId") Long especialidadId,
                            @Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora,
                            @Param("estados") Collection<EstadoCita> estados, @Param("excluirId") Long excluirId);

    boolean existsByHorarioId(Long horarioId);

    boolean existsByHorarioIdAndEstadoIn(Long horarioId, Collection<EstadoCita> estados);

    /** Citas de una fecha en estados dados, sin notificación del tipo indicado (recordatorios, RF-12). */
    @Query("select c from Cita c join fetch c.horario h join fetch h.medico m join fetch m.usuario mu "
            + "join fetch m.especialidad e join fetch h.consultorio co join fetch c.paciente p "
            + "where h.fecha = :fecha and c.estado in :estados "
            + "and not exists (select n.id from Notificacion n where n.cita = c and n.tipo = :tipo)")
    List<Cita> findSinNotificacion(@Param("fecha") LocalDate fecha,
                                   @Param("estados") Collection<EstadoCita> estados,
                                   @Param("tipo") TipoNotificacion tipo);

    // ----- Reportes (RF-32, RF-33) -----

    @Query("select c.estado, count(c) from Cita c join c.horario h "
            + "where h.fecha between :desde and :hasta group by c.estado")
    List<Object[]> contarPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select count(c) from Cita c join c.horario h "
            + "where h.fecha between :desde and :hasta and c.reprogramaciones > 0")
    long contarReprogramadas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select e.id, e.nombreEspecialidad, count(c), sum(case when c.estado = :noAsistio then 1 else 0 end) "
            + "from Cita c join c.horario h join h.medico m join m.especialidad e "
            + "where h.fecha between :desde and :hasta and c.estado in :considerados "
            + "group by e.id, e.nombreEspecialidad order by e.nombreEspecialidad")
    List<Object[]> inasistenciaPorEspecialidad(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                               @Param("noAsistio") EstadoCita noAsistio,
                                               @Param("considerados") Collection<EstadoCita> considerados);
}
