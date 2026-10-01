package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.hermilio.citas.model.entity.Rol;
import pe.utp.hermilio.citas.model.enums.RolNombre;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreRol(RolNombre nombreRol);
}
