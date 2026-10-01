package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PacienteDto {

    private PacienteDto() {
    }

    /**
     * RF-01 (auto-registro en la app) y RF-17 (registro por recepción).
     * En el auto-registro la contraseña es obligatoria; en recepción es opcional.
     */
    public record RegistroPacienteRequest(
            @NotBlank(message = "El documento de identidad es obligatorio")
            @Size(min = 8, max = 15, message = "El documento debe tener entre 8 y 15 caracteres") String documentoIdentidad,
            @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 150) String correo,
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @Size(max = 50) String tipoSeguro,
            @Size(max = 20) String telefonoContacto,
            @Size(max = 255) String datosApoderado) {
    }

    /** RF-04: actualización del perfil personal del paciente. */
    public record ActualizarPerfilRequest(
            @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 150) String correo,
            @Size(max = 50) String tipoSeguro,
            @Size(max = 20) String telefonoContacto,
            @Size(max = 255) String datosApoderado) {
    }

    public record PacienteResponse(Long idPaciente, Long idUsuario, String documentoIdentidad, String nombres,
                                   String apellidos, String correo, String tipoSeguro, String telefonoContacto,
                                   String datosApoderado) {
    }
}
