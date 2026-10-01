package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.HistorialClinico;

import java.util.List;
import java.util.Optional;

public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, Long> {

    String FETCH = "select hc from HistorialClinico hc join fetch hc.cita c join fetch c.paciente p "
            + "join fetch c.horario h join fetch h.medico m join fetch m.usuario mu join fetch m.especialidad e ";

    @Query(FETCH + "where hc.id = :id")
    Optional<HistorialClinico> findDetalleById(@Param("id") Long id);

    /** Historial clínico electrónico de un paciente, del más reciente al más antiguo (RF-28). */
    @Query(FETCH + "where p.id = :pacienteId order by hc.fechaRegistro desc")
    List<HistorialClinico> findByPaciente(@Param("pacienteId") Long pacienteId);

    boolean existsByCitaId(Long citaId);
}
