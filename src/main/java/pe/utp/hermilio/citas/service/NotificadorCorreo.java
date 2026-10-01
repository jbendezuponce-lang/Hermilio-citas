package pe.utp.hermilio.citas.service;

/** Puerto de salida para el envío del enlace/token de recuperación de contraseña (RF-03). */
public interface NotificadorCorreo {

    void enviarRecuperacion(String correo, String token);
}
