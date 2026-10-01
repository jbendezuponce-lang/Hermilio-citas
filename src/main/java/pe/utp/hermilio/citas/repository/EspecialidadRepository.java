package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.hermilio.citas.model.entity.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    boolean existsByNombreEspecialidadIgnoreCase(String nombreEspecialidad);
}
