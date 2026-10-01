package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.HorarioDto.CrearHorarioRequest;
import pe.utp.hermilio.citas.dto.HorarioDto.HorarioResponse;
import pe.utp.hermilio.citas.dto.HorarioDto.ModificarHorarioRequest;
import pe.utp.hermilio.citas.model.enums.Turno;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.HorarioService;

import java.time.LocalDate;
import java.util.List;

/** RF-06, RF-19, RF-34. */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    /** RF-06: horarios libres por especialidad (y opcionalmente fecha / turno). 200 | 404. */
    @GetMapping
    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA','ADMINISTRADOR')")
    public List<HorarioResponse> disponibles(@RequestParam Long especialidadId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                             @RequestParam(required = false) Turno turno) {
        return horarioService.buscarDisponibles(especialidadId, fecha, turno);
    }

    /** Horarios de un médico entre dos fechas, libres u ocupados. 200 | 400 | 404. */
    @GetMapping("/medico/{idMedico}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMINISTRADOR')")
    public List<HorarioResponse> porMedico(@PathVariable Long idMedico,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return horarioService.listarPorMedico(idMedico, desde, hasta);
    }

    /** RF-34. 201 | 400 | 404 | 409. */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<HorarioResponse> crear(@AuthenticationPrincipal UsuarioDetails actor,
                                                 @Valid @RequestBody CrearHorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(actor, request));
    }

    /** RF-19. 200 | 400 | 404 | 409. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMINISTRADOR')")
    public HorarioResponse modificar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                     @Valid @RequestBody ModificarHorarioRequest request) {
        return horarioService.modificar(actor, id, request);
    }

    /** 204 | 404 | 409 (si tiene citas). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        horarioService.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }
}
