package pe.utp.hermilio.citas.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioDisponibleRepository extends JpaRepository<HorarioDisponible, Long> {

    /**
     * Bloqueo pesimista del horario mientras se reserva (evita colisiones entre usuarios
     * simultáneos, según el diagrama de secuencia del documento).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioDisponible h where h.id = :id")
    Optional<HorarioDisponible> findByIdForUpdate(@Param("id") Long id);

    /** Horarios libres de una especialidad en un rango de fechas (RF-06). */
    @Query("select h from HorarioDisponible h join fetch h.medico m join fetch m.usuario u "
            + "join fetch m.especialidad e join fetch h.consultorio c "
            + "where h.disponible = true and e.id = :especialidadId and h.fecha between :desde and :hasta "
            + "order by h.fecha, h.horaInicio")
    List<HorarioDisponible> findDisponibles(@Param("especialidadId") Long especialidadId,
                                            @Param("desde") LocalDate desde,
                                            @Param("hasta") LocalDate hasta);

    /** Horarios de un médico (libres u ocupados) para su gestión (RF-19). */
    @Query("select h from HorarioDisponible h join fetch h.medico m join fetch m.usuario u "
            + "join fetch m.especialidad e join fetch h.consultorio c "
            + "where m.id = :medicoId and h.fecha between :desde and :hasta "
            + "order by h.fecha, h.horaInicio")
    List<HorarioDisponible> findByMedico(@Param("medicoId") Long medicoId,
                                         @Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta);

    @Query("select count(h) > 0 from HorarioDisponible h where h.medico.id = :medicoId and h.fecha = :fecha "
            + "and h.horaInicio < :fin and h.horaFin > :inicio and h.id <> :excluirId")
    boolean existeSolapeMedico(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha,
                               @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin,
                               @Param("excluirId") Long excluirId);

    @Query("select count(h) > 0 from HorarioDisponible h where h.consultorio.id = :consultorioId and h.fecha = :fecha "
            + "and h.horaInicio < :fin and h.horaFin > :inicio and h.id <> :excluirId")
    boolean existeSolapeConsultorio(@Param("consultorioId") Long consultorioId, @Param("fecha") LocalDate fecha,
                                    @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin,
                                    @Param("excluirId") Long excluirId);
}
