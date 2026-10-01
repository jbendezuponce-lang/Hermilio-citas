package pe.utp.hermilio.citas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto: no envía correos; solo deja constancia en el log. Reemplazar por un
 * proveedor SMTP/API (otro bean de NotificadorCorreo) al integrar el servicio de correo del hospital.
 * El token solo se muestra a nivel DEBUG para pruebas locales.
 */
@Component
public class LogNotificadorCorreo implements NotificadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(LogNotificadorCorreo.class);

    @Override
    public void enviarRecuperacion(String correo, String token) {
        log.info("Recuperación de contraseña solicitada para {} (sin proveedor de correo configurado)", correo);
        log.debug("Token de restablecimiento para {}: {}", correo, token);
    }
}
