package pe.utp.hermilio.citas.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.UsuarioDto.ActualizarUsuarioRequest;
import pe.utp.hermilio.citas.dto.UsuarioDto.CrearUsuarioRequest;
import pe.utp.hermilio.citas.dto.UsuarioDto.MedicoResponse;
import pe.utp.hermilio.citas.dto.UsuarioDto.UsuarioResponse;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Administrativo;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Rol;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.repository.AdministrativoRepository;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.repository.MedicoRepository;
import pe.utp.hermilio.citas.repository.RolRepository;
import pe.utp.hermilio.citas.repository.UsuarioRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.util.List;

/** RF-31: gestión de cuentas y roles por el administrador. */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final MedicoRepository medicoRepository;
    private final AdministrativoRepository administrativoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          MedicoRepository medicoRepository, AdministrativoRepository administrativoRepository,
                          EspecialidadRepository especialidadRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.medicoRepository = medicoRepository;
        this.administrativoRepository = administrativoRepository;
        this.especialidadRepository = especialidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    public List<UsuarioResponse> listar(RolNombre rol) {
        List<Usuario> usuarios = rol == null ? usuarioRepository.findAllConRol() : usuarioRepository.findByRol(rol);
        return usuarios.stream().map(this::aRespuesta).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    /** Médicos activos, opcionalmente de una especialidad (filtros de agenda y catálogo para la app). */
    public List<MedicoResponse> listarMedicos(Long especialidadId) {
        List<Medico> medicos = especialidadId == null
                ? medicoRepository.findActivos() : medicoRepository.findActivosByEspecialidad(especialidadId);
        return medicos.stream().map(DtoMapper::medico).toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioDetails actor, CrearUsuarioRequest r) {
        if (r.rol() == RolNombre.PACIENTE) {
            throw new BusinessRuleException("Los pacientes se registran desde la app (RF-01) o en recepción (RF-17)");
        }
        String documento = r.documentoIdentidad().trim();
        String correo = r.correo().trim().toLowerCase();
        if (usuarioRepository.existsByDocumentoIdentidad(documento)) {
            throw new ConflictException("Ya existe un usuario con ese documento de identidad");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        Especialidad especialidad = null;
        if (r.rol() == RolNombre.MEDICO) {
            if (r.idEspecialidad() == null || r.numeroColegiatura() == null || r.numeroColegiatura().isBlank()) {
                throw new BusinessRuleException("Para un médico la especialidad y el número de colegiatura son obligatorios");
            }
            especialidad = buscarEspecialidad(r.idEspecialidad());
            if (medicoRepository.existsByNumeroColegiatura(r.numeroColegiatura().trim())) {
                throw new ConflictException("Ya existe un médico con ese número de colegiatura");
            }
        }
        Rol rol = rolRepository.findByNombreRol(r.rol())
                .orElseThrow(() -> new IllegalStateException("El rol " + r.rol() + " no está inicializado"));
        Usuario usuario = usuarioRepository.save(new Usuario(rol, documento, r.nombres().trim(), r.apellidos().trim(),
                correo, passwordEncoder.encode(r.password())));
        if (r.rol() == RolNombre.MEDICO) {
            medicoRepository.save(new Medico(usuario, especialidad, r.numeroColegiatura().trim()));
        } else {
            String cargo = r.cargo() != null && !r.cargo().isBlank() ? r.cargo().trim()
                    : (r.rol() == RolNombre.RECEPCIONISTA ? "Recepcionista" : "Administrador");
            administrativoRepository.save(new Administrativo(usuario, cargo));
        }
        auditoriaService.registrar(actor.getId(), "CREAR_USUARIO usuario=" + usuario.getId() + " rol=" + r.rol());
        return aRespuesta(usuario);
    }

    /**
     * Actualiza datos de la cuenta. El rol solo puede cambiar entre RECEPCIONISTA y ADMINISTRADOR
     * (ambos usan el perfil Administrativo); un médico o paciente conserva su rol.
     */
    @Transactional
    public UsuarioResponse actualizar(UsuarioDetails actor, Long id, ActualizarUsuarioRequest r) {
        Usuario usuario = buscar(id);
        String correo = r.correo().trim().toLowerCase();
        if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        RolNombre actual = usuario.getRol().getNombreRol();
        if (r.rol() != null && r.rol() != actual) {
            boolean ambosAdministrativos = esAdministrativo(actual) && esAdministrativo(r.rol());
            if (!ambosAdministrativos) {
                throw new BusinessRuleException("Solo se puede cambiar el rol entre RECEPCIONISTA y ADMINISTRADOR");
            }
            if (usuario.getId().equals(actor.getId()) && r.rol() != RolNombre.ADMINISTRADOR) {
                throw new BusinessRuleException("No puede quitarse a sí mismo el rol de administrador");
            }
            usuario.setRol(rolRepository.findByNombreRol(r.rol())
                    .orElseThrow(() -> new IllegalStateException("El rol " + r.rol() + " no está inicializado")));
            actual = r.rol();
        }
        usuario.setNombres(r.nombres().trim());
        usuario.setApellidos(r.apellidos().trim());
        usuario.setCorreo(correo);

        if (actual == RolNombre.MEDICO) {
            Medico medico = medicoRepository.findByUsuarioId(id)
                    .orElseThrow(() -> new NotFoundException("El usuario no tiene perfil de médico"));
            if (r.idEspecialidad() != null) {
                medico.setEspecialidad(buscarEspecialidad(r.idEspecialidad()));
            }
            if (r.numeroColegiatura() != null && !r.numeroColegiatura().isBlank()
                    && !r.numeroColegiatura().trim().equals(medico.getNumeroColegiatura())) {
                if (medicoRepository.existsByNumeroColegiatura(r.numeroColegiatura().trim())) {
                    throw new ConflictException("Ya existe un médico con ese número de colegiatura");
                }
                medico.setNumeroColegiatura(r.numeroColegiatura().trim());
            }
        } else if (esAdministrativo(actual) && r.cargo() != null && !r.cargo().isBlank()) {
            administrativoRepository.findByUsuarioId(id).ifPresent(a -> a.setCargo(r.cargo().trim()));
        }
        auditoriaService.registrar(actor.getId(), "ACTUALIZAR_USUARIO usuario=" + id);
        return aRespuesta(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(UsuarioDetails actor, Long id, boolean activo) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(actor.getId()) && !activo) {
            throw new BusinessRuleException("No puede desactivar su propia cuenta");
        }
        usuario.setEstado(activo);
        auditoriaService.registrar(actor.getId(), (activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO") + " usuario=" + id);
        return aRespuesta(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new NotFoundException("El usuario no existe"));
    }

    private Especialidad buscarEspecialidad(Long id) {
        return especialidadRepository.findById(id).orElseThrow(() -> new NotFoundException("La especialidad no existe"));
    }

    private static boolean esAdministrativo(RolNombre rol) {
        return rol == RolNombre.RECEPCIONISTA || rol == RolNombre.ADMINISTRADOR;
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        RolNombre rol = u.getRol().getNombreRol();
        Long idEspecialidad = null;
        String especialidad = null;
        String colegiatura = null;
        String cargo = null;
        if (rol == RolNombre.MEDICO) {
            Medico m = medicoRepository.findByUsuarioId(u.getId()).orElse(null);
            if (m != null) {
                idEspecialidad = m.getEspecialidad().getId();
                especialidad = m.getEspecialidad().getNombreEspecialidad();
                colegiatura = m.getNumeroColegiatura();
            }
        } else if (esAdministrativo(rol)) {
            cargo = administrativoRepository.findByUsuarioId(u.getId()).map(Administrativo::getCargo).orElse(null);
        }
        return new UsuarioResponse(u.getId(), u.getDocumentoIdentidad(), u.getNombres(), u.getApellidos(), u.getCorreo(),
                rol, u.isEstado(), idEspecialidad, especialidad, colegiatura, cargo);
    }
}
