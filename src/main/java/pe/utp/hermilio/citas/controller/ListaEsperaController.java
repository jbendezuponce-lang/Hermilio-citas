package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.ListaEsperaDto.AgregarListaEsperaRequest;
import pe.utp.hermilio.citas.dto.ListaEsperaDto.ListaEsperaResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.ListaEsperaService;

import java.util.List;

/** RF-24: gestión de la lista de espera por especialidad (recepción). */
@RestController
@RequestMapping("/api/lista-espera")
@PreAuthorize("hasRole('RECEPCIONISTA')")
public class ListaEsperaController {

    private final ListaEsperaService listaEsperaService;

    public ListaEsperaController(ListaEsperaService listaEsperaService) {
        this.listaEsperaService = listaEsperaService;
    }

    /** 201 | 400 | 404 | 409. */
    @PostMapping
    public ResponseEntity<ListaEsperaResponse> agregar(@AuthenticationPrincipal UsuarioDetails actor,
                                                       @Valid @RequestBody AgregarListaEsperaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listaEsperaService.agregar(actor, request.idPaciente(), request.idEspecialidad()));
    }

    /** 200. */
    @GetMapping
    public List<ListaEsperaResponse> listar(@RequestParam(required = false) Long especialidadId) {
        return listaEsperaService.listar(especialidadId);
    }

    /** 200 | 404 | 409. */
    @PatchMapping("/{id}/atender")
    public ListaEsperaResponse atender(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return listaEsperaService.marcarAtendida(actor, id);
    }

    /** 204 | 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        listaEsperaService.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }
}
