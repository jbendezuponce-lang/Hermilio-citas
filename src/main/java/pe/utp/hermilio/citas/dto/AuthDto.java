package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.utp.hermilio.citas.model.enums.RolNombre;

public final class AuthDto {

    private AuthDto() {
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record LoginResponse(String token, String tipo, long expiraEnSegundos, Long idUsuario,
                                String nombres, String apellidos, RolNombre rol) {
    }

    public record RecuperarPasswordRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo) {
    }

    public record RestablecerPasswordRequest(
            @NotBlank(message = "El token es obligatorio") String token,
            @NotBlank(message = "La nueva contraseña es obligatoria")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String nuevaPassword) {
    }

    public record MensajeResponse(String mensaje) {
    }
}
