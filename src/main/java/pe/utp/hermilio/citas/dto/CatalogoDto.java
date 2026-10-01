package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs de los catálogos configurables por el administrador (RF-34). */
public final class CatalogoDto {

    private CatalogoDto() {
    }

    public record EspecialidadRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombreEspecialidad,
            @Size(max = 255) String descripcion) {
    }

    public record EspecialidadResponse(Long idEspecialidad, String nombreEspecialidad, String descripcion) {
    }

    public record ConsultorioRequest(
            @NotBlank(message = "El pabellón es obligatorio") @Size(max = 20) String numeroPabellon,
            @NotBlank(message = "El código de room es obligatorio") @Size(max = 20) String codigoRoom) {
    }

    public record ConsultorioResponse(Long idConsultorio, String numeroPabellon, String codigoRoom) {
    }
}
