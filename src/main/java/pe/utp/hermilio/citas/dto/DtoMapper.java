package pe.utp.hermilio.citas.dto;

import pe.utp.hermilio.citas.dto.CatalogoDto.ConsultorioResponse;
import pe.utp.hermilio.citas.dto.CatalogoDto.EspecialidadResponse;
import pe.utp.hermilio.citas.dto.CitaDto.CitaResponse;
import pe.utp.hermilio.citas.dto.ClinicoDto.HistorialResponse;
import pe.utp.hermilio.citas.dto.ClinicoDto.RecetaResponse;
import pe.utp.hermilio.citas.dto.ClinicoDto.TriajeResponse;
import pe.utp.hermilio.citas.dto.HorarioDto.HorarioResponse;
import pe.utp.hermilio.citas.dto.ListaEsperaDto.ListaEsperaResponse;
import pe.utp.hermilio.citas.dto.NotificacionDto.NotificacionResponse;
import pe.utp.hermilio.citas.dto.PacienteDto.PacienteResponse;
import pe.utp.hermilio.citas.dto.ReporteDto.AuditoriaResponse;
import pe.utp.hermilio.citas.dto.UsuarioDto.MedicoResponse;
import pe.utp.hermilio.citas.model.entity.AuditoriaLog;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.HistorialClinico;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.ListaEspera;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Notificacion;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.entity.Triaje;

/**
 * Conversión entidad -> DTO. Debe invocarse dentro de una transacción (las asociaciones son LAZY);
 * todos los servicios que lo usan son @Transactional. Así las entidades nunca se exponen por la API.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static PacienteResponse paciente(Paciente p) {
        return new PacienteResponse(p.getId(), p.getUsuario().getId(), p.getUsuario().getDocumentoIdentidad(),
                p.getUsuario().getNombres(), p.getUsuario().getApellidos(), p.getUsuario().getCorreo(),
                p.getTipoSeguro(), p.getTelefonoContacto(), p.getDatosApoderado());
    }

    public static MedicoResponse medico(Medico m) {
        return new MedicoResponse(m.getId(), m.getUsuario().getId(), m.getUsuario().getNombres(),
                m.getUsuario().getApellidos(), m.getEspecialidad().getId(),
                m.getEspecialidad().getNombreEspecialidad(), m.getNumeroColegiatura());
    }

    public static EspecialidadResponse especialidad(Especialidad e) {
        return new EspecialidadResponse(e.getId(), e.getNombreEspecialidad(), e.getDescripcion());
    }

    public static ConsultorioResponse consultorio(Consultorio c) {
        return new ConsultorioResponse(c.getId(), c.getNumeroPabellon(), c.getCodigoRoom());
    }

    public static HorarioResponse horario(HorarioDisponible h) {
        Medico m = h.getMedico();
        return new HorarioResponse(h.getId(), m.getId(), m.getUsuario().getNombreCompleto(),
                m.getEspecialidad().getId(), m.getEspecialidad().getNombreEspecialidad(),
                h.getConsultorio().getId(), h.getConsultorio().getNumeroPabellon(), h.getConsultorio().getCodigoRoom(),
                h.getFecha(), h.getHoraInicio(), h.getHoraFin(), h.getTurno(), h.isDisponible());
    }

    public static CitaResponse cita(Cita c) {
        HorarioDisponible h = c.getHorario();
        Medico m = h.getMedico();
        Paciente p = c.getPaciente();
        return new CitaResponse(c.getId(), c.getEstado(), c.getOrigen(), c.getReprogramaciones(), c.getFechaHoraReserva(),
                p.getId(), p.getUsuario().getNombreCompleto(), p.getUsuario().getDocumentoIdentidad(),
                m.getId(), m.getUsuario().getNombreCompleto(),
                m.getEspecialidad().getId(), m.getEspecialidad().getNombreEspecialidad(),
                h.getId(), h.getFecha(), h.getHoraInicio(), h.getHoraFin(), h.getTurno(),
                h.getConsultorio().getId(), h.getConsultorio().getNumeroPabellon(), h.getConsultorio().getCodigoRoom());
    }

    public static ListaEsperaResponse listaEspera(ListaEspera l) {
        return new ListaEsperaResponse(l.getId(), l.getPaciente().getId(), l.getPaciente().getUsuario().getNombreCompleto(),
                l.getPaciente().getUsuario().getDocumentoIdentidad(), l.getEspecialidad().getId(),
                l.getEspecialidad().getNombreEspecialidad(), l.getFechaRegistro(), l.getEstado());
    }

    public static TriajeResponse triaje(Triaje t) {
        return new TriajeResponse(t.getId(), t.getCita().getId(), t.getPresionArterial(), t.getPeso(),
                t.getSignosVitales(), t.getFechaRegistro());
    }

    public static HistorialResponse historial(HistorialClinico hc) {
        Cita c = hc.getCita();
        Medico m = c.getHorario().getMedico();
        return new HistorialResponse(hc.getId(), c.getId(), c.getHorario().getFecha(), c.getPaciente().getId(),
                m.getUsuario().getNombreCompleto(), m.getEspecialidad().getNombreEspecialidad(),
                hc.getDiagnostico(), hc.getTratamiento(), hc.getObservaciones(), hc.getFechaRegistro(),
                hc.getRecetas().stream()
                        .map(r -> new RecetaResponse(r.getId(), r.getMedicamento(), r.getDosis(), r.getIndicaciones()))
                        .toList());
    }

    public static NotificacionResponse notificacion(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getCita().getId(), n.getTipo(), n.getMensaje(),
                n.getFechaEnvio(), n.isLeido());
    }

    public static AuditoriaResponse auditoria(AuditoriaLog l) {
        return new AuditoriaResponse(l.getId(),
                l.getUsuario() != null ? l.getUsuario().getId() : null,
                l.getUsuario() != null ? l.getUsuario().getCorreo() : null,
                l.getAccion(), l.getFechaHora());
    }
}
