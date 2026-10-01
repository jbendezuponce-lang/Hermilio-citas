package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialActualizarRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.HistorialService;

import java.util.List;

/** RF-28 y RF-29. Solo el rol MEDICO accede al historial clínico (RNF-02). */
@RestController
@RequestMapping("/api/historiales")
@PreAuthorize("hasRole('MEDICO')")
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    /** RF-29. 201 | 400 | 403 | 404 | 409. */
    @PostMapping
    public ResponseEntity<HistorialResponse> registrar(@AuthenticationPrincipal UsuarioDetails actor,
                                                       @Valid @RequestBody HistorialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(historialService.registrar(actor, request));
    }

    /** 200 | 400 | 403 | 404. */
    @PutMapping("/{id}")
    public HistorialResponse actualizar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                        @Valid @RequestBody HistorialActualizarRequest request) {
        return historialService.actualizar(actor, id, request);
    }

    /** RF-28. 200 | 404. */
    @GetMapping("/paciente/{idPaciente}")
    public List<HistorialResponse> delPaciente(@AuthenticationPrincipal UsuarioDetails actor,
                                               @PathVariable Long idPaciente) {
        return historialService.historialDelPaciente(actor, idPaciente);
    }
}
