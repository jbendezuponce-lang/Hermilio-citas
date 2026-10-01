package pe.utp.hermilio.citas.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.CatalogoDto.EspecialidadRequest;
import pe.utp.hermilio.citas.dto.CatalogoDto.EspecialidadResponse;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.util.List;

/** RF-05 (consulta) y RF-34 (configuración por el administrador). */
@Service
@Transactional(readOnly = true)
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final AuditoriaService auditoriaService;

    public EspecialidadService(EspecialidadRepository especialidadRepository, AuditoriaService auditoriaService) {
        this.especialidadRepository = especialidadRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<EspecialidadResponse> listar() {
        return especialidadRepository.findAll(Sort.by("nombreEspecialidad")).stream().map(DtoMapper::especialidad).toList();
    }

    @Transactional
    public EspecialidadResponse crear(UsuarioDetails actor, EspecialidadRequest r) {
        String nombre = r.nombreEspecialidad().trim();
        if (especialidadRepository.existsByNombreEspecialidadIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una especialidad con ese nombre");
        }
        Especialidad e = especialidadRepository.save(new Especialidad(nombre, r.descripcion()));
        auditoriaService.registrar(actor.getId(), "CREAR_ESPECIALIDAD id=" + e.getId());
        return DtoMapper.especialidad(e);
    }

    @Transactional
    public EspecialidadResponse actualizar(UsuarioDetails actor, Long id, EspecialidadRequest r) {
        Especialidad e = buscar(id);
        String nombre = r.nombreEspecialidad().trim();
        if (!nombre.equalsIgnoreCase(e.getNombreEspecialidad()) && especialidadRepository.existsByNombreEspecialidadIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una especialidad con ese nombre");
        }
        e.setNombreEspecialidad(nombre);
        e.setDescripcion(r.descripcion());
        auditoriaService.registrar(actor.getId(), "ACTUALIZAR_ESPECIALIDAD id=" + id);
        return DtoMapper.especialidad(e);
    }

    /** Si tiene médicos, horarios o lista de espera asociados la BD lo impide y se responde 409. */
    @Transactional
    public void eliminar(UsuarioDetails actor, Long id) {
        Especialidad e = buscar(id);
        especialidadRepository.delete(e);
        especialidadRepository.flush();
        auditoriaService.registrar(actor.getId(), "ELIMINAR_ESPECIALIDAD id=" + id);
    }

    private Especialidad buscar(Long id) {
        return especialidadRepository.findById(id).orElseThrow(() -> new NotFoundException("La especialidad no existe"));
    }
}
