package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Administrativo;

import java.util.Optional;

public interface AdministrativoRepository extends JpaRepository<Administrativo, Long> {

    @Query("select a from Administrativo a join fetch a.usuario u where u.id = :usuarioId")
    Optional<Administrativo> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
