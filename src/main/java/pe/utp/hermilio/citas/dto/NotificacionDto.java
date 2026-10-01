package pe.utp.hermilio.citas.dto;

import pe.utp.hermilio.citas.model.enums.TipoNotificacion;

import java.time.LocalDateTime;

public final class NotificacionDto {

    private NotificacionDto() {
    }

    public record NotificacionResponse(Long idNotificacion, Long idCita, TipoNotificacion tipo, String mensaje,
                                       LocalDateTime fechaEnvio, boolean leido) {
    }
}
