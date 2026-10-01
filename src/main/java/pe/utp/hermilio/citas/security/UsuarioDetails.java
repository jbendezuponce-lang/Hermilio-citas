package pe.utp.hermilio.citas.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;

import java.util.Collection;
import java.util.List;

/** Principal autenticado: se inyecta en los controladores con @AuthenticationPrincipal. */
public class UsuarioDetails implements UserDetails {

    private final Long id;
    private final String correo;
    private final String passwordHash;
    private final boolean activo;
    private final RolNombre rol;

    public UsuarioDetails(Long id, String correo, String passwordHash, boolean activo, RolNombre rol) {
        this.id = id;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.activo = activo;
        this.rol = rol;
    }

    public static UsuarioDetails de(Usuario u) {
        return new UsuarioDetails(u.getId(), u.getCorreo(), u.getPasswordHash(), u.isEstado(), u.getRol().getNombreRol());
    }

    public Long getId() { return id; }
    public RolNombre getRol() { return rol; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return correo; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return activo; }
}
