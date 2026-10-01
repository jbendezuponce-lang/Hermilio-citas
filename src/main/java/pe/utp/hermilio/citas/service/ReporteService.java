package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.ReporteDto.FilaInasistencia;
import pe.utp.hermilio.citas.dto.ReporteDto.ReporteCitasResponse;
import pe.utp.hermilio.citas.dto.ReporteDto.ReporteInasistenciaResponse;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.repository.CitaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** RF-32 (citas atendidas / canceladas / reprogramadas) y RF-33 (absentismo por especialidad y periodo). */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final CitaRepository citaRepository;

    public ReporteService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    public ReporteCitasResponse reporteCitas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Map<EstadoCita, Long> porEstado = new EnumMap<>(EstadoCita.class);
        for (Object[] fila : citaRepository.contarPorEstado(desde, hasta)) {
            porEstado.put((EstadoCita) fila[0], ((Number) fila[1]).longValue());
        }
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        return new ReporteCitasResponse(desde, hasta, total,
                porEstado.getOrDefault(EstadoCita.ATENDIDA, 0L),
                porEstado.getOrDefault(EstadoCita.CANCELADA, 0L),
                citaRepository.contarReprogramadas(desde, hasta),
                porEstado.getOrDefault(EstadoCita.NO_ASISTIO, 0L));
    }

    /**
     * Porcentaje = inasistencias / (atendidas + con check-in + inasistencias). Las canceladas no cuentan porque
     * liberaron el cupo con anticipación.
     */
    public ReporteInasistenciaResponse reporteInasistencia(LocalDate desde, LocalDate hasta, Long idEspecialidad) {
        validarPeriodo(desde, hasta);
        List<FilaInasistencia> filas = new ArrayList<>();
        for (Object[] f : citaRepository.inasistenciaPorEspecialidad(desde, hasta, EstadoCita.NO_ASISTIO,
                EstadoCita.CUMPLIDAS_O_PERDIDAS)) {
            Long id = ((Number) f[0]).longValue();
            if (idEspecialidad != null && !idEspecialidad.equals(id)) {
                continue;
            }
            long consideradas = ((Number) f[2]).longValue();
            long inasistencias = f[3] == null ? 0L : ((Number) f[3]).longValue();
            double porcentaje = consideradas == 0 ? 0.0 : Math.round(inasistencias * 10000.0 / consideradas) / 100.0;
            filas.add(new FilaInasistencia(id, (String) f[1], consideradas, inasistencias, porcentaje));
        }
        return new ReporteInasistenciaResponse(desde, hasta, filas);
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }
}
