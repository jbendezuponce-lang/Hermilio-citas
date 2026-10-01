package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.HorarioDto.CrearHorarioRequest;
import pe.utp.hermilio.citas.dto.HorarioDto.HorarioResponse;
import pe.utp.hermilio.citas.dto.HorarioDto.ModificarHorarioRequest;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.Turno;
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.ConsultorioRepository;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.repository.HorarioDisponibleRepository;
import pe.utp.hermilio.citas.repository.MedicoRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** RF-06 (consulta), RF-19 (modificar disponibilidad), RF-34 (configurar turnos). */
@Service
@Transactional(readOnly = true)
public class HorarioService {

    /** Ventana de búsqueda por defecto cuando no se indica fecha. */
    static final int DIAS_VENTANA_BUSQUEDA = 30;
    static final LocalTime CORTE_TURNO = LocalTime.of(13, 0);
    private static final long SIN_EXCLUSION = -1L;

    private final HorarioDisponibleRepository horarioRepository;
    private final MedicoRepository medicoRepository;
    private final ConsultorioRepository consultorioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final CitaRepository citaRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public HorarioService(HorarioDisponibleRepository horarioRepository, MedicoRepository medicoRepository,
                          ConsultorioRepository consultorioRepository, EspecialidadRepository especialidadRepository,
                          CitaRepository citaRepository, AuditoriaService auditoriaService, Clock clock) {
        this.horarioRepository = horarioRepository;
        this.medicoRepository = medicoRepository;
        this.consultorioRepository = consultorioRepository;
        this.especialidadRepository = especialidadRepository;
        this.citaRepository = citaRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    /** RF-06: horarios libres por especialidad y, opcionalmente, fecha y turno. Sin fecha: próximos 30 días. */
    public List<HorarioResponse> buscarDisponibles(Long especialidadId, LocalDate fecha, Turno turno) {
        if (!especialidadRepository.existsById(especialidadId)) {
            throw new NotFoundException("La especialidad no existe");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        LocalDate desde = fecha != null ? fecha : hoy;
        LocalDate hasta = fecha != null ? fecha : hoy.plusDays(DIAS_VENTANA_BUSQUEDA);
        if (desde.isBefore(hoy)) {
            return List.of();
        }
        return horarioRepository.findDisponibles(especialidadId, desde, hasta).stream()
                .filter(h -> LocalDateTime.of(h.getFecha(), h.getHoraInicio()).isAfter(ahora))
                .filter(h -> turno == null || h.getTurno() == turno)
                .map(DtoMapper::horario)
                .toList();
    }

    /** Horarios (libres y ocupados) de un médico, para su gestión por recepción / administración. */
    public List<HorarioResponse> listarPorMedico(Long medicoId, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (!medicoRepository.existsById(medicoId)) {
            throw new NotFoundException("El médico no existe");
        }
        return horarioRepository.findByMedico(medicoId, desde, hasta).stream().map(DtoMapper::horario).toList();
    }

    @Transactional
    public HorarioResponse crear(UsuarioDetails actor, CrearHorarioRequest r) {
        Medico medico = medicoRepository.findById(r.idMedico()).orElseThrow(() -> new NotFoundException("El médico no existe"));
        if (!medico.getUsuario().isEstado()) {
            throw new BusinessRuleException("El médico está inactivo");
        }
        Consultorio consultorio = buscarConsultorio(r.idConsultorio());
        validarRango(r.fecha(), r.horaInicio(), r.horaFin());
        validarSolapes(medico.getId(), consultorio.getId(), r.fecha(), r.horaInicio(), r.horaFin(), SIN_EXCLUSION);
        HorarioDisponible h = horarioRepository.save(new HorarioDisponible(medico, consultorio, r.fecha(),
                r.horaInicio(), r.horaFin(), turnoDe(r.horaInicio())));
        auditoriaService.registrar(actor.getId(), "CREAR_HORARIO horario=" + h.getId());
        return DtoMapper.horario(h);
    }

    /** RF-19. Un horario con cita activa no puede modificarse: primero debe cancelarse o reprogramarse la cita. */
    @Transactional
    public HorarioResponse modificar(UsuarioDetails actor, Long id, ModificarHorarioRequest r) {
        HorarioDisponible h = horarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("El horario no existe"));
        if (citaRepository.existsByHorarioIdAndEstadoIn(id, EstadoCita.ACTIVAS)) {
            throw new ConflictException("El horario tiene una cita activa; cancele o reprograme la cita antes de modificarlo");
        }
        Consultorio consultorio = buscarConsultorio(r.idConsultorio());
        boolean cambiaFranja = !r.fecha().equals(h.getFecha()) || !r.horaInicio().equals(h.getHoraInicio())
                || !r.horaFin().equals(h.getHoraFin());
        if (cambiaFranja) {
            validarRango(r.fecha(), r.horaInicio(), r.horaFin());
        }
        validarSolapes(h.getMedico().getId(), consultorio.getId(), r.fecha(), r.horaInicio(), r.horaFin(), h.getId());
        h.setConsultorio(consultorio);
        h.setFecha(r.fecha());
        h.setHoraInicio(r.horaInicio());
        h.setHoraFin(r.horaFin());
        h.setTurno(turnoDe(r.horaInicio()));
        h.setDisponible(r.disponible());
        auditoriaService.registrar(actor.getId(), "MODIFICAR_HORARIO horario=" + id);
        return DtoMapper.horario(h);
    }

    @Transactional
    public void eliminar(UsuarioDetails actor, Long id) {
        HorarioDisponible h = horarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("El horario no existe"));
        if (citaRepository.existsByHorarioId(id)) {
            throw new ConflictException("El horario tiene citas asociadas; márquelo como no disponible en lugar de eliminarlo");
        }
        horarioRepository.delete(h);
        auditoriaService.registrar(actor.getId(), "ELIMINAR_HORARIO horario=" + id);
    }

    static Turno turnoDe(LocalTime horaInicio) {
        return horaInicio.isBefore(CORTE_TURNO) ? Turno.MANANA : Turno.TARDE;
    }

    private void validarRango(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (!LocalDateTime.of(fecha, inicio).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("No se pueden programar horarios en el pasado");
        }
    }

    private void validarSolapes(Long medicoId, Long consultorioId, LocalDate fecha, LocalTime inicio, LocalTime fin, Long excluirId) {
        if (horarioRepository.existeSolapeMedico(medicoId, fecha, inicio, fin, excluirId)) {
            throw new ConflictException("El médico ya tiene un horario que se cruza con esa franja");
        }
        if (horarioRepository.existeSolapeConsultorio(consultorioId, fecha, inicio, fin, excluirId)) {
            throw new ConflictException("El consultorio ya está asignado en esa franja");
        }
    }

    private Consultorio buscarConsultorio(Long id) {
        return consultorioRepository.findById(id).orElseThrow(() -> new NotFoundException("El consultorio no existe"));
    }
}
