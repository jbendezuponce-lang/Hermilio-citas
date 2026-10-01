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
import pe.utp.hermilio.citas.dto.CatalogoDto.ConsultorioRequest;
import pe.utp.hermilio.citas.dto.CatalogoDto.ConsultorioResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.ConsultorioService;

import java.util.List;

@RestController
@RequestMapping("/api/consultorios")
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    public ConsultorioController(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    /** 200. */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMINISTRADOR')")
    public List<ConsultorioResponse> listar() {
        return consultorioService.listar();
    }

    /** 201 | 400 | 409. */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConsultorioResponse> crear(@AuthenticationPrincipal UsuarioDetails actor,
                                                     @Valid @RequestBody ConsultorioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultorioService.crear(actor, request));
    }

    /** 200 | 400 | 404 | 409. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConsultorioResponse actualizar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                          @Valid @RequestBody ConsultorioRequest request) {
        return consultorioService.actualizar(actor, id, request);
    }
}
