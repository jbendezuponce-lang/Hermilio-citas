package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.ClinicoDto.TriajeRequest;
import pe.utp.hermilio.citas.dto.ClinicoDto.TriajeResponse;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.Triaje;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.TriajeRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDateTime;

/** Registro de signos vitales previo a la consulta (KPI "Tiempos de Triaje"; carril Recepción / Triaje). */
@Service
@Transactional(readOnly = true)
public class TriajeService {

    private final TriajeRepository triajeRepository;
    private final CitaRepository citaRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public TriajeService(TriajeRepository triajeRepository, CitaRepository citaRepository,
                         AuditoriaService auditoriaService, Clock clock) {
        this.triajeRepository = triajeRepository;
        this.citaRepository = citaRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional
    public TriajeResponse registrar(UsuarioDetails actor, TriajeRequest r) {
        if (r.presionArterial() == null && r.peso() == null
                && (r.signosVitales() == null || r.signosVitales().isBlank())) {
            throw new BusinessRuleException("Debe registrar al menos un dato de triaje");
        }
        Cita cita = citaRepository.findById(r.idCita()).orElseThrow(() -> new NotFoundException("La cita no existe"));
        if (cita.getEstado() != EstadoCita.CHECK_IN) {
            throw new ConflictException("El triaje solo puede registrarse a pacientes con check-in");
        }
        if (triajeRepository.existsByCitaId(cita.getId())) {
            throw new ConflictException("La cita ya tiene un triaje registrado");
        }
        Triaje t = triajeRepository.save(new Triaje(cita, r.presionArterial(), r.peso(), r.signosVitales(),
                LocalDateTime.now(clock)));
        auditoriaService.registrar(actor.getId(), "REGISTRAR_TRIAJE cita=" + cita.getId());
        return DtoMapper.triaje(t);
    }

    /** No es readOnly porque el acceso se audita (INSERT en auditoria_logs). */
    @Transactional
    public TriajeResponse obtenerPorCita(UsuarioDetails actor, Long idCita) {
        Triaje t = triajeRepository.findByCitaId(idCita)
                .orElseThrow(() -> new NotFoundException("La cita no tiene triaje registrado"));
        auditoriaService.registrar(actor.getId(), "CONSULTA_TRIAJE cita=" + idCita);
        return DtoMapper.triaje(t);
    }
}
