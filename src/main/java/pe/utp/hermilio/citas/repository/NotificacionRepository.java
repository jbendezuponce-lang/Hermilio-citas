package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("select n from Notificacion n join fetch n.cita c where c.paciente.id = :pacienteId order by n.fechaEnvio desc")
    List<Notificacion> findByPaciente(@Param("pacienteId") Long pacienteId);

    @Query("select n from Notificacion n join fetch n.cita c join fetch c.paciente p join fetch p.usuario u where n.id = :id")
    Optional<Notificacion> findDetalleById(@Param("id") Long id);
}
