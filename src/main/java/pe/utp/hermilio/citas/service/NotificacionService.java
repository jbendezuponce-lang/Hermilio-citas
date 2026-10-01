package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.NotificacionDto.NotificacionResponse;
import pe.utp.hermilio.citas.exception.ForbiddenOperationException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Notificacion;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.TipoNotificacion;
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.NotificacionRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF-11, RF-12, RF-13. Las notificaciones se persisten y la app móvil las consulta; el envío push a un proveedor
 * externo (FCM) queda fuera del alcance del backend base.
 */
@Service
public class NotificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificacionRepository, PacienteRepository pacienteRepository,
                               CitaRepository citaRepository, Clock clock) {
        this.notificacionRepository = notificacionRepository;
        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
        this.clock = clock;
    }

    @Transactional
    public void notificar(Cita cita, TipoNotificacion tipo) {
        notificacionRepository.save(new Notificacion(cita, tipo, construirMensaje(cita, tipo), LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones(UsuarioDetails actor) {
        Paciente paciente = pacienteRepository.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new NotFoundException("No existe un perfil de paciente para el usuario"));
        return notificacionRepository.findByPaciente(paciente.getId()).stream().map(DtoMapper::notificacion).toList();
    }

    @Transactional
    public NotificacionResponse marcarLeida(UsuarioDetails actor, Long idNotificacion) {
        Notificacion n = notificacionRepository.findDetalleById(idNotificacion)
                .orElseThrow(() -> new NotFoundException("La notificación no existe"));
        if (!n.getCita().getPaciente().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("La notificación pertenece a otro paciente");
        }
        n.setLeido(true);
        return DtoMapper.notificacion(n);
    }

    /** RF-12: genera el recordatorio de las citas activas de la fecha indicada que aún no lo tengan. */
    @Transactional
    public int generarRecordatorios(LocalDate fechaCitas) {
        List<Cita> citas = citaRepository.findSinNotificacion(fechaCitas, EstadoCita.MODIFICABLES, TipoNotificacion.RECORDATORIO);
        citas.forEach(c -> notificar(c, TipoNotificacion.RECORDATORIO));
        return citas.size();
    }

    private String construirMensaje(Cita cita, TipoNotificacion tipo) {
        HorarioDisponible h = cita.getHorario();
        String detalle = h.getMedico().getEspecialidad().getNombreEspecialidad()
                + " con " + h.getMedico().getUsuario().getNombreCompleto()
                + " el " + FECHA.format(h.getFecha()) + " a las " + HORA.format(h.getHoraInicio())
                + " (Pabellón " + h.getConsultorio().getNumeroPabellon()
                + ", Room " + h.getConsultorio().getCodigoRoom() + ")";
        return switch (tipo) {
            case CONFIRMACION -> "Su cita fue confirmada: " + detalle + ".";
            case RECORDATORIO -> "Recordatorio: tiene una cita de " + detalle + ".";
            case REPROGRAMACION -> "Su cita fue reprogramada. Nueva cita: " + detalle + ".";
            case CANCELACION -> "Su cita fue cancelada: " + detalle + ".";
        };
    }
}
