package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Carga el usuario con su rol en una sola consulta (login y filtro JWT). */
    @Query("select u from Usuario u join fetch u.rol where lower(u.correo) = lower(:correo)")
    Optional<Usuario> findByCorreoConRol(@Param("correo") String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByRolNombreRol(RolNombre nombreRol);

    @Query("select u from Usuario u join fetch u.rol r order by u.apellidos, u.nombres")
    List<Usuario> findAllConRol();

    @Query("select u from Usuario u join fetch u.rol r where r.nombreRol = :rol order by u.apellidos, u.nombres")
    List<Usuario> findByRol(@Param("rol") RolNombre rol);
}
