package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.utp.hermilio.citas.model.enums.RolNombre;

public final class UsuarioDto {

    private UsuarioDto() {
    }

    /** RF-31: alta de personal (RECEPCIONISTA, MEDICO o ADMINISTRADOR). Los pacientes se registran por RF-01/RF-17. */
    public record CrearUsuarioRequest(
            @NotBlank @Size(min = 8, max = 15) String documentoIdentidad,
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 150) String correo,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @NotNull(message = "El rol es obligatorio") RolNombre rol,
            Long idEspecialidad,
            @Size(max = 20) String numeroColegiatura,
            @Size(max = 80) String cargo) {
    }

    public record ActualizarUsuarioRequest(
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 150) String correo,
            RolNombre rol,
            Long idEspecialidad,
            @Size(max = 20) String numeroColegiatura,
            @Size(max = 80) String cargo) {
    }

    public record CambiarEstadoRequest(@NotNull(message = "El estado es obligatorio") Boolean activo) {
    }

    public record UsuarioResponse(Long idUsuario, String documentoIdentidad, String nombres, String apellidos,
                                  String correo, RolNombre rol, boolean activo,
                                  Long idEspecialidad, String especialidad, String numeroColegiatura, String cargo) {
    }

    public record MedicoResponse(Long idMedico, Long idUsuario, String nombres, String apellidos,
                                 Long idEspecialidad, String especialidad, String numeroColegiatura) {
    }
}
