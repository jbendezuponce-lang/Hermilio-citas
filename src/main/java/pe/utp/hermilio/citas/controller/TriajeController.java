package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.ClinicoDto.TriajeRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.TriajeResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.TriajeService;

@RestController
@RequestMapping("/api/triajes")
@PreAuthorize("hasAnyRole('RECEPCIONISTA','MEDICO')")
public class TriajeController {

    private final TriajeService triajeService;

    public TriajeController(TriajeService triajeService) {
        this.triajeService = triajeService;
    }

    /** 201 | 400 | 404 | 409. */
    @PostMapping
    public ResponseEntity<TriajeResponse> registrar(@AuthenticationPrincipal UsuarioDetails actor,
                                                    @Valid @RequestBody TriajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triajeService.registrar(actor, request));
    }

    /** 200 | 404. */
    @GetMapping("/cita/{idCita}")
    public TriajeResponse porCita(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long idCita) {
        return triajeService.obtenerPorCita(actor, idCita);
    }
}
