package pe.utp.hermilio.citas.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.ReporteDto.AuditoriaResponse;
import pe.utp.hermilio.citas.dto.ReporteDto.ReporteCitasResponse;
import pe.utp.hermilio.citas.dto.ReporteDto.ReporteInasistenciaResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.AuditoriaService;
import pe.utp.hermilio.citas.service.ReporteService;

import java.time.LocalDate;
import java.util.List;

/** RF-32, RF-33 y consulta de la auditoría (RNF-14). Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteController {

    private final ReporteService reporteService;
    private final AuditoriaService auditoriaService;

    public ReporteController(ReporteService reporteService, AuditoriaService auditoriaService) {
        this.reporteService = reporteService;
        this.auditoriaService = auditoriaService;
    }

    /** RF-32. 200 | 400. */
    @GetMapping("/reportes/citas")
    public ReporteCitasResponse citas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.reporteCitas(desde, hasta);
    }

    /** RF-33. 200 | 400. */
    @GetMapping("/reportes/inasistencia")
    public ReporteInasistenciaResponse inasistencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long especialidadId) {
        return reporteService.reporteInasistencia(desde, hasta, especialidadId);
    }

    /** RNF-14: registros de auditoría (máx. 500, del más reciente al más antiguo). 200 | 400. */
    @GetMapping("/auditoria")
    public List<AuditoriaResponse> auditoria(
            @AuthenticationPrincipal UsuarioDetails actor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long usuarioId) {
        return auditoriaService.listar(desde, hasta, usuarioId);
    }
}
