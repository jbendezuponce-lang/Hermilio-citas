package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.NotNull;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.OrigenCita;
import pe.utp.hermilio.citas.model.enums.Turno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class CitaDto {

    private CitaDto() {
    }

    /** RF-07: reserva desde la app móvil (el paciente es el usuario autenticado). */
    public record ReservarCitaRequest(@NotNull(message = "El horario es obligatorio") Long idHorario) {
    }

    /** RF-20: registro presencial por recepción en nombre del paciente. */
    public record ReservarCitaPresencialRequest(
            @NotNull(message = "El paciente es obligatorio") Long idPaciente,
            @NotNull(message = "El horario es obligatorio") Long idHorario) {
    }

    /** RF-08 / RF-21: reprogramación a otro horario libre de la misma especialidad. */
    public record ReprogramarCitaRequest(@NotNull(message = "El nuevo horario es obligatorio") Long idNuevoHorario) {
    }

    /** Incluye la ubicación del consultorio (RF-14). */
    public record CitaResponse(Long idCita, EstadoCita estado, OrigenCita origen, int reprogramaciones,
                               LocalDateTime fechaHoraReserva,
                               Long idPaciente, String paciente, String documentoPaciente,
                               Long idMedico, String medico, Long idEspecialidad, String especialidad,
                               Long idHorario, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Turno turno,
                               Long idConsultorio, String numeroPabellon, String codigoRoom) {
    }
}
