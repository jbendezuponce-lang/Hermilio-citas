package pe.utp.hermilio.citas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Triaje, historial clínico electrónico y recetas. */
public final class ClinicoDto {

    private ClinicoDto() {
    }

    public record TriajeRequest(
            @NotNull(message = "La cita es obligatoria") Long idCita,
            @Pattern(regexp = "^\\d{2,3}/\\d{2,3}$", message = "La presión arterial debe tener formato 120/80") String presionArterial,
            @DecimalMin(value = "0.5", message = "Peso inválido") @DecimalMax(value = "500", message = "Peso inválido") BigDecimal peso,
            @Size(max = 255) String signosVitales) {
    }

    public record TriajeResponse(Long idTriaje, Long idCita, String presionArterial, BigDecimal peso,
                                 String signosVitales, LocalDateTime fechaRegistro) {
    }

    public record RecetaRequest(
            @NotBlank(message = "El medicamento es obligatorio") @Size(max = 150) String medicamento,
            @NotBlank(message = "La dosis es obligatoria") @Size(max = 100) String dosis,
            @Size(max = 500) String indicaciones) {
    }

    public record RecetaResponse(Long idReceta, String medicamento, String dosis, String indicaciones) {
    }

    /** RF-29: tratamiento / indicaciones de la consulta (y diagnóstico opcional). */
    public record HistorialRequest(
            @NotNull(message = "La cita es obligatoria") Long idCita,
            String diagnostico,
            @NotBlank(message = "El tratamiento o las indicaciones son obligatorios") String tratamiento,
            String observaciones,
            @Valid List<RecetaRequest> recetas) {
    }

    public record HistorialActualizarRequest(
            String diagnostico,
            @NotBlank(message = "El tratamiento o las indicaciones son obligatorios") String tratamiento,
            String observaciones,
            @Valid List<RecetaRequest> recetas) {
    }

    public record HistorialResponse(Long idHistorial, Long idCita, LocalDate fechaCita, Long idPaciente,
                                    String medico, String especialidad,
                                    String diagnostico, String tratamiento, String observaciones,
                                    LocalDateTime fechaRegistro, List<RecetaResponse> recetas) {
    }
}
