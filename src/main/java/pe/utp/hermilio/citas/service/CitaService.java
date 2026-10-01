package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.CitaDto.CitaResponse;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.ForbiddenOperationException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.OrigenCita;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.model.enums.TipoNotificacion;
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.HorarioDisponibleRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Núcleo del sistema: reserva, reprogramación, cancelación, check-in, atención y agendas
 * (RF-07 a RF-10, RF-14, RF-20 a RF-23, RF-25, RF-27, RF-30, RF-35).
 */
@Service
@Transactional(readOnly = true)
public class CitaService {

    private static final long SIN_EXCLUSION = -1L;

    private final CitaRepository citaRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final PacienteRepository pacienteRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public CitaService(CitaRepository citaRepository, HorarioDisponibleRepository horarioRepository,
                       PacienteRepository pacienteRepository, NotificacionService notificacionService,
                       AuditoriaService auditoriaService, Clock clock) {
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ reservar

    /** RF-07: el paciente autenticado reserva desde la app móvil. */
    @Transactional
    public CitaResponse reservar(UsuarioDetails actor, Long idHorario) {
        Paciente paciente = pacienteRepository.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new NotFoundException("No existe un perfil de paciente para el usuario"));
        Cita cita = crearCita(paciente, idHorario, OrigenCita.MOVIL);
        auditoriaService.registrar(actor.getId(), "RESERVAR_CITA cita=" + cita.getId());
        return DtoMapper.cita(cita);
    }

    /** RF-20: recepción registra la cita presencial en nombre del paciente. */
    @Transactional
    public CitaResponse reservarPresencial(UsuarioDetails actor, Long idPaciente, Long idHorario) {
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new NotFoundException("El paciente no existe"));
        if (!paciente.getUsuario().isEstado()) {
            throw new BusinessRuleException("La cuenta del paciente está inactiva");
        }
        Cita cita = crearCita(paciente, idHorario, OrigenCita.PRESENCIAL);
        auditoriaService.registrar(actor.getId(), "RESERVAR_CITA_PRESENCIAL cita=" + cita.getId());
        return DtoMapper.cita(cita);
    }

    private Cita crearCita(Paciente paciente, Long idHorario, OrigenCita origen) {
        HorarioDisponible horario = horarioRepository.findByIdForUpdate(idHorario)
                .orElseThrow(() -> new NotFoundException("El horario no existe"));
        validarHorarioReservable(horario);
        validarSinDuplicado(paciente.getId(), horario, SIN_EXCLUSION);
        Cita cita = citaRepository.save(
                new Cita(paciente, horario, LocalDateTime.now(clock), EstadoCita.CONFIRMADA, origen));
        horario.setDisponible(false);
        notificacionService.notificar(cita, TipoNotificacion.CONFIRMACION);
        return cita;
    }

    // ------------------------------------------------- reprogramar / cancelar

    /** RF-08 (paciente) y RF-21 (recepción, solo citas presenciales). */
    @Transactional
    public CitaResponse reprogramar(UsuarioDetails actor, Long idCita, Long idNuevoHorario) {
        Cita cita = citaOperable(actor, idCita);
        exigirEstado(cita, "Solo se pueden reprogramar citas confirmadas o ya reprogramadas");
        HorarioDisponible anterior = cita.getHorario();
        if (anterior.getId().equals(idNuevoHorario)) {
            throw new BusinessRuleException("El nuevo horario debe ser distinto al actual");
        }
        HorarioDisponible nuevo = horarioRepository.findByIdForUpdate(idNuevoHorario)
                .orElseThrow(() -> new NotFoundException("El nuevo horario no existe"));
        validarHorarioReservable(nuevo);
        Long espAnterior = anterior.getMedico().getEspecialidad().getId();
        Long espNueva = nuevo.getMedico().getEspecialidad().getId();
        if (!espAnterior.equals(espNueva)) {
            throw new BusinessRuleException("La reprogramación debe realizarse dentro de la misma especialidad");
        }
        validarSinDuplicado(cita.getPaciente().getId(), nuevo, cita.getId());

        anterior.setDisponible(true);
        nuevo.setDisponible(false);
        cita.setHorario(nuevo);
        cita.setEstado(EstadoCita.REPROGRAMADA);
        cita.setReprogramaciones(cita.getReprogramaciones() + 1);
        notificacionService.notificar(cita, TipoNotificacion.REPROGRAMACION);
        auditoriaService.registrar(actor.getId(), "REPROGRAMAR_CITA cita=" + idCita);
        return DtoMapper.cita(cita);
    }

    /** RF-09 (paciente) y RF-22 (recepción, solo citas presenciales). Libera el horario de inmediato. */
    @Transactional
    public CitaResponse cancelar(UsuarioDetails actor, Long idCita) {
        Cita cita = citaOperable(actor, idCita);
        exigirEstado(cita, "Solo se pueden cancelar citas confirmadas o reprogramadas");
        cita.setEstado(EstadoCita.CANCELADA);
        cita.getHorario().setDisponible(true);
        notificacionService.notificar(cita, TipoNotificacion.CANCELACION);
        auditoriaService.registrar(actor.getId(), "CANCELAR_CITA cita=" + idCita);
        return DtoMapper.cita(cita);
    }

    // ------------------------------------------------ flujo del día de la cita

    /** RF-23: recepción confirma la asistencia (check-in) el mismo día de la cita. */
    @Transactional
    public CitaResponse registrarCheckIn(UsuarioDetails actor, Long idCita) {
        Cita cita = buscarDetalle(idCita);
        exigirEstado(cita, "Solo se puede registrar check-in en citas confirmadas o reprogramadas");
        if (!cita.getHorario().getFecha().equals(LocalDate.now(clock))) {
            throw new ConflictException("El check-in solo puede registrarse el día de la cita");
        }
        cita.setEstado(EstadoCita.CHECK_IN);
        auditoriaService.registrar(actor.getId(), "CHECK_IN cita=" + idCita);
        return DtoMapper.cita(cita);
    }

    /** RF-30: el médico de la cita la marca como atendida (requiere check-in previo). */
    @Transactional
    public CitaResponse marcarAtendida(UsuarioDetails actor, Long idCita) {
        Cita cita = buscarDetalle(idCita);
        exigirMedicoDeLaCita(actor, cita);
        if (cita.getEstado() != EstadoCita.CHECK_IN) {
            throw new ConflictException("Solo se puede marcar como atendida una cita con check-in registrado");
        }
        cita.setEstado(EstadoCita.ATENDIDA);
        auditoriaService.registrar(actor.getId(), "CITA_ATENDIDA cita=" + idCita);
        return DtoMapper.cita(cita);
    }

    /** Fuente de datos del reporte de absentismo (RF-33). El horario permanece ocupado. */
    @Transactional
    public CitaResponse marcarNoAsistio(UsuarioDetails actor, Long idCita) {
        Cita cita = buscarDetalle(idCita);
        if (actor.getRol() == RolNombre.MEDICO) {
            exigirMedicoDeLaCita(actor, cita);
        }
        exigirEstado(cita, "Solo se puede marcar inasistencia en citas confirmadas o reprogramadas");
        if (cita.getHorario().getFecha().isAfter(LocalDate.now(clock))) {
            throw new ConflictException("No se puede marcar inasistencia de una cita futura");
        }
        cita.setEstado(EstadoCita.NO_ASISTIO);
        auditoriaService.registrar(actor.getId(), "CITA_NO_ASISTIO cita=" + idCita);
        return DtoMapper.cita(cita);
    }

    // ------------------------------------------------------------- consultas

    /** RF-10. */
    public List<CitaResponse> misCitas(UsuarioDetails actor) {
        Paciente paciente = pacienteRepository.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new NotFoundException("No existe un perfil de paciente para el usuario"));
        return citaRepository.findByPaciente(paciente.getId()).stream().map(DtoMapper::cita).toList();
    }

    /** RF-14: el detalle incluye pabellón y room del consultorio. */
    public CitaResponse obtener(UsuarioDetails actor, Long idCita) {
        Cita cita = buscarDetalle(idCita);
        switch (actor.getRol()) {
            case PACIENTE -> exigirPacienteDeLaCita(actor, cita);
            case MEDICO -> exigirMedicoDeLaCita(actor, cita);
            default -> { /* recepcionista y administrador consultan cualquier cita */ }
        }
        return DtoMapper.cita(cita);
    }

    /** RF-25: agenda de un día por médico y/o especialidad (recepción y administración). */
    public List<CitaResponse> agenda(LocalDate fecha, Long idMedico, Long idEspecialidad) {
        return citaRepository.findAgendaPorFecha(fecha).stream()
                .filter(c -> idMedico == null || idMedico.equals(c.getHorario().getMedico().getId()))
                .filter(c -> idEspecialidad == null
                        || idEspecialidad.equals(c.getHorario().getMedico().getEspecialidad().getId()))
                .map(DtoMapper::cita)
                .toList();
    }

    /** RF-27: agenda del día del médico autenticado. */
    public List<CitaResponse> agendaDelMedico(UsuarioDetails actor, LocalDate fecha) {
        return citaRepository.findAgendaPorFecha(fecha).stream()
                .filter(c -> c.getHorario().getMedico().getUsuario().getId().equals(actor.getId()))
                .map(DtoMapper::cita)
                .toList();
    }

    // -------------------------------------------------------- reglas internas

    private Cita buscarDetalle(Long idCita) {
        return citaRepository.findDetalleById(idCita).orElseThrow(() -> new NotFoundException("La cita no existe"));
    }

    /**
     * Quién puede reprogramar/cancelar: el paciente dueño de la cita o la recepción, esta última solo sobre citas
     * registradas de forma presencial (RF-21, RF-22).
     */
    private Cita citaOperable(UsuarioDetails actor, Long idCita) {
        Cita cita = buscarDetalle(idCita);
        if (actor.getRol() == RolNombre.PACIENTE) {
            exigirPacienteDeLaCita(actor, cita);
        } else if (actor.getRol() == RolNombre.RECEPCIONISTA) {
            if (cita.getOrigen() != OrigenCita.PRESENCIAL) {
                throw new ForbiddenOperationException("La recepción solo gestiona citas registradas de forma presencial");
            }
        } else {
            throw new ForbiddenOperationException("Su rol no puede modificar citas");
        }
        return cita;
    }

    private void exigirPacienteDeLaCita(UsuarioDetails actor, Cita cita) {
        if (!cita.getPaciente().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("La cita pertenece a otro paciente");
        }
    }

    private void exigirMedicoDeLaCita(UsuarioDetails actor, Cita cita) {
        if (!cita.getHorario().getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("La cita corresponde a otro médico");
        }
    }

    private void exigirEstado(Cita cita, String mensaje) {
        if (!EstadoCita.MODIFICABLES.contains(cita.getEstado())) {
            throw new ConflictException(mensaje + " (estado actual: " + cita.getEstado() + ")");
        }
    }

    private void validarHorarioReservable(HorarioDisponible horario) {
        if (!horario.isDisponible()) {
            throw new ConflictException("El horario seleccionado ya no está disponible");
        }
        if (!LocalDateTime.of(horario.getFecha(), horario.getHoraInicio()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("No se puede reservar un horario que ya pasó");
        }
    }

    /** RF-35: mismo paciente, especialidad, fecha y hora con una cita activa => 409. */
    private void validarSinDuplicado(Long idPaciente, HorarioDisponible horario, Long excluirCitaId) {
        boolean duplicada = citaRepository.existeDuplicada(idPaciente, horario.getMedico().getEspecialidad().getId(),
                horario.getFecha(), horario.getHoraInicio(), EstadoCita.ACTIVAS, excluirCitaId);
        if (duplicada) {
            throw new ConflictException(
                    "Ya existe una cita del paciente en esa especialidad, fecha y hora. Seleccione otro horario");
        }
    }
}
