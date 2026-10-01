package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialActualizarRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialResponse;
import pe.utp.hermilio.citas.dto.ClinicoDto.RecetaRequest;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.ForbiddenOperationException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.HistorialClinico;
import pe.utp.hermilio.citas.model.entity.RecetaMedica;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.HistorialClinicoRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Historial clínico electrónico (RF-28, RF-29). Cada acceso y cada modificación queda en la auditoría (RNF-14);
 * el acceso está restringido al rol MÉDICO (RNF-02, controlado en el controlador).
 */
@Service
@Transactional(readOnly = true)
public class HistorialService {

    private final HistorialClinicoRepository historialRepository;
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public HistorialService(HistorialClinicoRepository historialRepository, CitaRepository citaRepository,
                            PacienteRepository pacienteRepository, AuditoriaService auditoriaService, Clock clock) {
        this.historialRepository = historialRepository;
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    /** RF-29: el médico de la cita registra diagnóstico, tratamiento e indicaciones (con recetas opcionales). */
    @Transactional
    public HistorialResponse registrar(UsuarioDetails actor, HistorialRequest r) {
        Cita cita = citaRepository.findDetalleById(r.idCita()).orElseThrow(() -> new NotFoundException("La cita no existe"));
        exigirMedicoDeLaCita(actor, cita);
        if (cita.getEstado() != EstadoCita.CHECK_IN && cita.getEstado() != EstadoCita.ATENDIDA) {
            throw new ConflictException("Solo se registra historial en citas con check-in o atendidas");
        }
        if (historialRepository.existsByCitaId(cita.getId())) {
            throw new ConflictException("La cita ya tiene un registro en el historial clínico");
        }
        HistorialClinico hc = new HistorialClinico(cita, r.diagnostico(), r.tratamiento(), r.observaciones(),
                LocalDateTime.now(clock));
        agregarRecetas(hc, r.recetas());
        historialRepository.save(hc);
        auditoriaService.registrar(actor.getId(), "CREAR_HISTORIAL historial=" + hc.getId() + " paciente=" + cita.getPaciente().getId());
        return DtoMapper.historial(hc);
    }

    /** Actualiza el registro. recetas = null conserva las existentes; una lista (incluso vacía) las reemplaza. */
    @Transactional
    public HistorialResponse actualizar(UsuarioDetails actor, Long idHistorial, HistorialActualizarRequest r) {
        HistorialClinico hc = historialRepository.findDetalleById(idHistorial)
                .orElseThrow(() -> new NotFoundException("El registro de historial no existe"));
        exigirMedicoDeLaCita(actor, hc.getCita());
        hc.setDiagnostico(r.diagnostico());
        hc.setTratamiento(r.tratamiento());
        hc.setObservaciones(r.observaciones());
        if (r.recetas() != null) {
            hc.limpiarRecetas();
            agregarRecetas(hc, r.recetas());
        }
        auditoriaService.registrar(actor.getId(), "ACTUALIZAR_HISTORIAL historial=" + idHistorial
                + " paciente=" + hc.getCita().getPaciente().getId());
        return DtoMapper.historial(hc);
    }

    /**
     * RF-28: historial completo del paciente (todas sus consultas), del más reciente al más antiguo.
     * No es readOnly porque el acceso se audita (INSERT en auditoria_logs) dentro de la misma transacción.
     */
    @Transactional
    public List<HistorialResponse> historialDelPaciente(UsuarioDetails actor, Long idPaciente) {
        if (!pacienteRepository.existsById(idPaciente)) {
            throw new NotFoundException("El paciente no existe");
        }
        List<HistorialResponse> historial = historialRepository.findByPaciente(idPaciente).stream()
                .map(DtoMapper::historial).toList();
        auditoriaService.registrar(actor.getId(), "CONSULTA_HISTORIAL paciente=" + idPaciente);
        return historial;
    }

    private void exigirMedicoDeLaCita(UsuarioDetails actor, Cita cita) {
        if (!cita.getHorario().getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico que atiende la cita puede registrar su historial");
        }
    }

    private void agregarRecetas(HistorialClinico hc, List<RecetaRequest> recetas) {
        if (recetas == null) {
            return;
        }
        for (RecetaRequest rr : recetas) {
            hc.agregarReceta(new RecetaMedica(rr.medicamento().trim(), rr.dosis().trim(), rr.indicaciones()));
        }
    }
}
