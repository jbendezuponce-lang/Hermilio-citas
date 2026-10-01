package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Medico;

import java.util.List;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    @Query("select m from Medico m join fetch m.usuario u join fetch m.especialidad e where u.id = :usuarioId")
    Optional<Medico> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    boolean existsByNumeroColegiatura(String numeroColegiatura);

    boolean existsByEspecialidadId(Long especialidadId);

    @Query("select m from Medico m join fetch m.usuario u join fetch m.especialidad e "
            + "where u.estado = true order by u.apellidos, u.nombres")
    List<Medico> findActivos();

    @Query("select m from Medico m join fetch m.usuario u join fetch m.especialidad e "
            + "where u.estado = true and e.id = :especialidadId order by u.apellidos, u.nombres")
    List<Medico> findActivosByEspecialidad(@Param("especialidadId") Long especialidadId);
}
