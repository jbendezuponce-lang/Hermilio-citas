package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.NotNull;
import pe.utp.hermilio.citas.model.enums.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public final class HorarioDto {

    private HorarioDto() {
    }

    /** RF-34: alta de una franja de atención. El turno se deduce de la hora de inicio (antes de las 13:00 = MANANA). */
    public record CrearHorarioRequest(
            @NotNull(message = "El médico es obligatorio") Long idMedico,
            @NotNull(message = "El consultorio es obligatorio") Long idConsultorio,
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
            @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
    }

    /** RF-19: modificación de una franja existente (sin citas activas). */
    public record ModificarHorarioRequest(
            @NotNull(message = "El consultorio es obligatorio") Long idConsultorio,
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
            @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
            @NotNull(message = "Indique si el horario está disponible") Boolean disponible) {
    }

    public record HorarioResponse(Long idHorario, Long idMedico, String medico, Long idEspecialidad, String especialidad,
                                  Long idConsultorio, String numeroPabellon, String codigoRoom,
                                  LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Turno turno, boolean disponible) {
    }
}
