package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.UsuarioDto.ActualizarUsuarioRequest;
import pe.utp.hermilio.citas.dto.UsuarioDto.CambiarEstadoRequest;
import pe.utp.hermilio.citas.dto.UsuarioDto.CrearUsuarioRequest;
import pe.utp.hermilio.citas.dto.UsuarioDto.UsuarioResponse;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.UsuarioService;

import java.util.List;

/** RF-31: cuentas y roles (solo ADMINISTRADOR). */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** 200. Filtro opcional ?rol=MEDICO. */
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) RolNombre rol) {
        return usuarioService.listar(rol);
    }

    /** 200 | 404. */
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    /** 201 | 400 | 404 | 409. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@AuthenticationPrincipal UsuarioDetails actor,
                                                 @Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(actor, request));
    }

    /** 200 | 400 | 404 | 409. */
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                      @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.actualizar(actor, id, request);
    }

    /** Activa o desactiva la cuenta (no se borran usuarios: se conserva la trazabilidad). 200 | 400 | 404. */
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRequest request) {
        return usuarioService.cambiarEstado(actor, id, request.activo());
    }
}
