package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import pe.utp.hermilio.citas.dto.CitaDto.CitaResponse;
import pe.utp.hermilio.citas.dto.CitaDto.ReprogramarCitaRequest;
import pe.utp.hermilio.citas.dto.CitaDto.ReservarCitaPresencialRequest;
import pe.utp.hermilio.citas.dto.CitaDto.ReservarCitaRequest;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.CitaService;

import java.time.LocalDate;
import java.util.List;

/** RF-07 a RF-10, RF-14, RF-20 a RF-23, RF-25, RF-27, RF-30, RF-35. */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /** RF-07 / RF-35. 201 | 400 | 404 | 409 (horario ocupado o cita duplicada). */
    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponse> reservar(@AuthenticationPrincipal UsuarioDetails actor,
                                                 @Valid @RequestBody ReservarCitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.reservar(actor, request.idHorario()));
    }

    /** RF-20. 201 | 400 | 404 | 409. */
    @PostMapping("/presencial")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<CitaResponse> reservarPresencial(@AuthenticationPrincipal UsuarioDetails actor,
                                                           @Valid @RequestBody ReservarCitaPresencialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citaService.reservarPresencial(actor, request.idPaciente(), request.idHorario()));
    }

    /** RF-10: historial de citas del paciente autenticado. 200. */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<CitaResponse> misCitas(@AuthenticationPrincipal UsuarioDetails actor) {
        return citaService.misCitas(actor);
    }

    /** RF-25: agenda diaria por médico / especialidad. 200. */
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMINISTRADOR')")
    public List<CitaResponse> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                     @RequestParam(required = false) Long medicoId,
                                     @RequestParam(required = false) Long especialidadId) {
        return citaService.agenda(fecha, medicoId, especialidadId);
    }

    /** RF-27: agenda diaria del médico autenticado. 200. */
    @GetMapping("/agenda/mia")
    @PreAuthorize("hasRole('MEDICO')")
    public List<CitaResponse> miAgenda(@AuthenticationPrincipal UsuarioDetails actor,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.agendaDelMedico(actor, fecha);
    }

    /** RF-14: detalle con la ubicación del consultorio. 200 | 403 | 404. */
    @GetMapping("/{id}")
    public CitaResponse obtener(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return citaService.obtener(actor, id);
    }

    /** RF-08 / RF-21. 200 | 400 | 403 | 404 | 409. */
    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA')")
    public CitaResponse reprogramar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id,
                                    @Valid @RequestBody ReprogramarCitaRequest request) {
        return citaService.reprogramar(actor, id, request.idNuevoHorario());
    }

    /** RF-09 / RF-22. 200 | 403 | 404 | 409. */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA')")
    public CitaResponse cancelar(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return citaService.cancelar(actor, id);
    }

    /** RF-23. 200 | 404 | 409. */
    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public CitaResponse checkIn(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return citaService.registrarCheckIn(actor, id);
    }

    /** RF-30. 200 | 403 | 404 | 409. */
    @PatchMapping("/{id}/atender")
    @PreAuthorize("hasRole('MEDICO')")
    public CitaResponse atender(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return citaService.marcarAtendida(actor, id);
    }

    /** Registra la inasistencia (base del reporte RF-33). 200 | 403 | 404 | 409. */
    @PatchMapping("/{id}/no-asistio")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','MEDICO')")
    public CitaResponse noAsistio(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return citaService.marcarNoAsistio(actor, id);
    }
}
