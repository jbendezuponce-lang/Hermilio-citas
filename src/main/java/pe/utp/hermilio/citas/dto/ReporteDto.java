package pe.utp.hermilio.citas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReporteDto {

    private ReporteDto() {
    }

    /** RF-32: citas por resultado en un periodo (según la fecha de la cita). */
    public record ReporteCitasResponse(LocalDate desde, LocalDate hasta, long total, long atendidas,
                                       long canceladas, long reprogramadas, long noAsistio) {
    }

    /** RF-33: absentismo por especialidad. porcentaje = inasistencias / citas consideradas * 100. */
    public record FilaInasistencia(Long idEspecialidad, String especialidad, long citasConsideradas,
                                   long inasistencias, double porcentaje) {
    }

    public record ReporteInasistenciaResponse(LocalDate desde, LocalDate hasta, List<FilaInasistencia> filas) {
    }

    /** RNF-14: registro de trazabilidad. */
    public record AuditoriaResponse(Long idLog, Long idUsuario, String correoUsuario, String accion, LocalDateTime fechaHora) {
    }
}
