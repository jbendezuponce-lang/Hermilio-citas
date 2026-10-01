package pe.utp.hermilio.citas.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.NotificacionDto.NotificacionResponse;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.NotificacionService;

import java.util.List;

/** RF-11, RF-12, RF-13: la app móvil consulta las notificaciones automatizadas del paciente. */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasRole('PACIENTE')")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** 200. */
    @GetMapping("/mias")
    public List<NotificacionResponse> mias(@AuthenticationPrincipal UsuarioDetails actor) {
        return notificacionService.misNotificaciones(actor);
    }

    /** 200 | 403 | 404. */
    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@AuthenticationPrincipal UsuarioDetails actor, @PathVariable Long id) {
        return notificacionService.marcarLeida(actor, id);
    }
}
