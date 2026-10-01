package pe.utp.hermilio.citas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.ListaEsperaDto.ListaEsperaResponse;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.ListaEspera;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.enums.EstadoListaEspera;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.repository.ListaEsperaRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** RF-24: lista de espera de pacientes por especialidad (entidad intermedia Paciente-Especialidad). */
@Service
@Transactional(readOnly = true)
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadRepository especialidadRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ListaEsperaService(ListaEsperaRepository listaEsperaRepository, PacienteRepository pacienteRepository,
                              EspecialidadRepository especialidadRepository, AuditoriaService auditoriaService, Clock clock) {
        this.listaEsperaRepository = listaEsperaRepository;
        this.pacienteRepository = pacienteRepository;
        this.especialidadRepository = especialidadRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional
    public ListaEsperaResponse agregar(UsuarioDetails actor, Long idPaciente, Long idEspecialidad) {
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new NotFoundException("El paciente no existe"));
        Especialidad especialidad = especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new NotFoundException("La especialidad no existe"));
        if (listaEsperaRepository.existsByPacienteIdAndEspecialidadIdAndEstado(idPaciente, idEspecialidad, EstadoListaEspera.EN_ESPERA)) {
            throw new ConflictException("El paciente ya está en la lista de espera de esa especialidad");
        }
        ListaEspera l = listaEsperaRepository.save(new ListaEspera(paciente, especialidad, LocalDateTime.now(clock)));
        auditoriaService.registrar(actor.getId(), "AGREGAR_LISTA_ESPERA id=" + l.getId());
        return DtoMapper.listaEspera(l);
    }

    /** Pacientes en espera, por orden de llegada; opcionalmente de una sola especialidad. */
    public List<ListaEsperaResponse> listar(Long idEspecialidad) {
        List<ListaEspera> lista = idEspecialidad == null
                ? listaEsperaRepository.findByEstado(EstadoListaEspera.EN_ESPERA)
                : listaEsperaRepository.findByEstadoYEspecialidad(EstadoListaEspera.EN_ESPERA, idEspecialidad);
        return lista.stream().map(DtoMapper::listaEspera).toList();
    }

    /** Marca que al paciente ya se le asignó una cita. */
    @Transactional
    public ListaEsperaResponse marcarAtendida(UsuarioDetails actor, Long id) {
        ListaEspera l = buscar(id);
        if (l.getEstado() == EstadoListaEspera.ATENDIDA) {
            throw new ConflictException("La solicitud ya fue atendida");
        }
        l.setEstado(EstadoListaEspera.ATENDIDA);
        auditoriaService.registrar(actor.getId(), "ATENDER_LISTA_ESPERA id=" + id);
        return DtoMapper.listaEspera(l);
    }

    @Transactional
    public void eliminar(UsuarioDetails actor, Long id) {
        listaEsperaRepository.delete(buscar(id));
        auditoriaService.registrar(actor.getId(), "ELIMINAR_LISTA_ESPERA id=" + id);
    }

    private ListaEspera buscar(Long id) {
        return listaEsperaRepository.findDetalleById(id)
                .orElseThrow(() -> new NotFoundException("La solicitud de lista de espera no existe"));
    }
}
