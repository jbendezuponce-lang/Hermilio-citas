package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.PacienteDto.ActualizarPerfilRequest;
import pe.utp.hermilio.citas.dto.PacienteDto.PacienteResponse;
import pe.utp.hermilio.citas.dto.PacienteDto.RegistroPacienteRequest;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.PacienteService;

/** RF-04, RF-17, RF-18. */
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    /** Perfil del paciente autenticado. 200 | 404. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse miPerfil(@AuthenticationPrincipal UsuarioDetails actor) {
        return pacienteService.obtenerPropio(actor);
    }

    /** RF-04. 200 | 400 | 409. */
    @PutMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse actualizarPerfil(@AuthenticationPrincipal UsuarioDetails actor,
                                             @Valid @RequestBody ActualizarPerfilRequest request) {
        return pacienteService.actualizarPropio(actor, request);
    }

    /** RF-17: recepción registra un paciente nuevo. 201 | 400 | 409. */
    @PostMapping
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<PacienteResponse> registrar(@AuthenticationPrincipal UsuarioDetails actor,
                                                      @Valid @RequestBody RegistroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.registrar(request, false, actor));
    }

    /** RF-18: búsqueda por documento de identidad. 200 | 404. */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','MEDICO','ADMINISTRADOR')")
    public PacienteResponse buscarPorDocumento(@RequestParam String documento) {
        return pacienteService.buscarPorDocumento(documento);
    }
}
