package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Paciente;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    @Query("select p from Paciente p join fetch p.usuario u where u.id = :usuarioId")
    Optional<Paciente> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("select p from Paciente p join fetch p.usuario u where u.documentoIdentidad = :documento")
    Optional<Paciente> findByDocumento(@Param("documento") String documento);
}
