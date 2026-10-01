package pe.utp.hermilio.citas.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.CatalogoDto.ConsultorioRequest;
import pe.utp.hermilio.citas.dto.CatalogoDto.ConsultorioResponse;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.repository.ConsultorioRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.util.List;

/** Catálogo de consultorios (pabellón + room), necesario para publicar horarios y mostrar la ubicación (RF-14). */
@Service
@Transactional(readOnly = true)
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final AuditoriaService auditoriaService;

    public ConsultorioService(ConsultorioRepository consultorioRepository, AuditoriaService auditoriaService) {
        this.consultorioRepository = consultorioRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<ConsultorioResponse> listar() {
        return consultorioRepository.findAll(Sort.by("numeroPabellon", "codigoRoom")).stream()
                .map(DtoMapper::consultorio).toList();
    }

    @Transactional
    public ConsultorioResponse crear(UsuarioDetails actor, ConsultorioRequest r) {
        String pabellon = r.numeroPabellon().trim();
        String room = r.codigoRoom().trim();
        if (consultorioRepository.existsByNumeroPabellonAndCodigoRoom(pabellon, room)) {
            throw new ConflictException("Ya existe un consultorio con ese pabellón y room");
        }
        Consultorio c = consultorioRepository.save(new Consultorio(pabellon, room));
        auditoriaService.registrar(actor.getId(), "CREAR_CONSULTORIO id=" + c.getId());
        return DtoMapper.consultorio(c);
    }

    @Transactional
    public ConsultorioResponse actualizar(UsuarioDetails actor, Long id, ConsultorioRequest r) {
        Consultorio c = consultorioRepository.findById(id).orElseThrow(() -> new NotFoundException("El consultorio no existe"));
        String pabellon = r.numeroPabellon().trim();
        String room = r.codigoRoom().trim();
        boolean cambia = !pabellon.equals(c.getNumeroPabellon()) || !room.equals(c.getCodigoRoom());
        if (cambia && consultorioRepository.existsByNumeroPabellonAndCodigoRoom(pabellon, room)) {
            throw new ConflictException("Ya existe un consultorio con ese pabellón y room");
        }
        c.setNumeroPabellon(pabellon);
        c.setCodigoRoom(room);
        auditoriaService.registrar(actor.getId(), "ACTUALIZAR_CONSULTORIO id=" + id);
        return DtoMapper.consultorio(c);
    }
}
