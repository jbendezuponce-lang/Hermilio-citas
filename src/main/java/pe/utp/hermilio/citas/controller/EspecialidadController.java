package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.CatalogoDto.EspecialidadRequest;
import pe.utp.hermilio.citas.dto.CatalogoDto.EspecialidadResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.EspecialidadService;

import java.util.List;

/** RF-05 (consulta, cualquier usuario autenticado) y RF-34 (alta/edición/baja, ADMINISTRADOR). */
@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    /** 200. */
    @GetMapping
    public List<EspecialidadResponse> listar() {
        return especialidadService.listar();
    }

    /** 201 | 400 | 409. */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EspecialidadResponse> crear(@AuthenticationPrincipal UsuarioDetails actor,
                                                      @Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(actor, request));
    }

    /** 200 | 400 | 404 | 409. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EspecialidadResponse actualizar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                           @Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.actualizar(actor, id, request);
    }

    /** 204 | 404 | 409 (si tiene médicos, horarios o lista de espera asociados). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        especialidadService.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }
}
