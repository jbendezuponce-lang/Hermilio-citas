package pe.utp.hermilio.citas.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.PacienteDto.ActualizarPerfilRequest;
import pe.utp.hermilio.citas.dto.PacienteDto.PacienteResponse;
import pe.utp.hermilio.citas.dto.PacienteDto.RegistroPacienteRequest;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.NotFoundException;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.entity.Rol;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.repository.RolRepository;
import pe.utp.hermilio.citas.repository.UsuarioRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository,
                           RolRepository rolRepository, PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    /**
     * RF-01 (autoRegistro = true, contraseña obligatoria) y RF-17 (registro por recepción, contraseña opcional).
     * Si recepción no define contraseña, se guarda una aleatoria y el paciente la establece con RF-03.
     */
    @Transactional
    public PacienteResponse registrar(RegistroPacienteRequest r, boolean autoRegistro, UsuarioDetails actor) {
        boolean sinPassword = r.password() == null || r.password().isBlank();
        if (autoRegistro && sinPassword) {
            throw new BusinessRuleException("La contraseña es obligatoria");
        }
        String documento = r.documentoIdentidad().trim();
        String correo = r.correo().trim().toLowerCase();
        if (usuarioRepository.existsByDocumentoIdentidad(documento)) {
            throw new ConflictException("Ya existe un usuario con ese documento de identidad");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        Rol rol = rolRepository.findByNombreRol(RolNombre.PACIENTE)
                .orElseThrow(() -> new IllegalStateException("El rol PACIENTE no está inicializado"));
        String password = sinPassword ? UUID.randomUUID().toString() : r.password();
        Usuario usuario = usuarioRepository.save(new Usuario(rol, documento, r.nombres().trim(), r.apellidos().trim(),
                correo, passwordEncoder.encode(password)));
        Paciente paciente = pacienteRepository.save(
                new Paciente(usuario, r.tipoSeguro(), r.telefonoContacto(), r.datosApoderado()));
        auditoriaService.registrar(actor != null ? actor.getId() : usuario.getId(), "REGISTRAR_PACIENTE paciente=" + paciente.getId());
        return DtoMapper.paciente(paciente);
    }

    public PacienteResponse obtenerPropio(UsuarioDetails actor) {
        return DtoMapper.paciente(pacientePropio(actor));
    }

    /** RF-04. */
    @Transactional
    public PacienteResponse actualizarPropio(UsuarioDetails actor, ActualizarPerfilRequest r) {
        Paciente paciente = pacientePropio(actor);
        Usuario usuario = paciente.getUsuario();
        String correo = r.correo().trim().toLowerCase();
        if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        usuario.setNombres(r.nombres().trim());
        usuario.setApellidos(r.apellidos().trim());
        usuario.setCorreo(correo);
        paciente.setTipoSeguro(r.tipoSeguro());
        paciente.setTelefonoContacto(r.telefonoContacto());
        paciente.setDatosApoderado(r.datosApoderado());
        auditoriaService.registrar(actor.getId(), "ACTUALIZAR_PERFIL paciente=" + paciente.getId());
        return DtoMapper.paciente(paciente);
    }

    /** RF-18. */
    public PacienteResponse buscarPorDocumento(String documento) {
        return pacienteRepository.findByDocumento(documento.trim())
                .map(DtoMapper::paciente)
                .orElseThrow(() -> new NotFoundException("No se encontró un paciente con ese documento de identidad"));
    }

    private Paciente pacientePropio(UsuarioDetails actor) {
        return pacienteRepository.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new NotFoundException("No existe un perfil de paciente para el usuario"));
    }
}
