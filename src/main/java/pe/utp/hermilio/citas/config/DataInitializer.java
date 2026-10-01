package pe.utp.hermilio.citas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.model.entity.Administrativo;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.entity.Rol;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.model.enums.Turno;
import pe.utp.hermilio.citas.repository.AdministrativoRepository;
import pe.utp.hermilio.citas.repository.ConsultorioRepository;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.repository.HorarioDisponibleRepository;
import pe.utp.hermilio.citas.repository.MedicoRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.repository.RolRepository;
import pe.utp.hermilio.citas.repository.UsuarioRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Arranque idempotente:
 * <ol>
 *   <li>Garantiza los 4 roles.</li>
 *   <li>Si se configuró por variables de entorno y aún no existe ningún administrador, crea el primero (RF-31).</li>
 *   <li>Si app.bootstrap.cuentas-demo=true, garantiza las cuentas de prueba y repara su contraseña cuando el hash
 *       guardado no corresponde a app.bootstrap.password-demo (por ejemplo, hashes pegados a mano en un script SQL).</li>
 * </ol>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdministrativoRepository administrativoRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultorioRepository consultorioRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrap;

    public DataInitializer(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                           AdministrativoRepository administrativoRepository, MedicoRepository medicoRepository,
                           PacienteRepository pacienteRepository, EspecialidadRepository especialidadRepository,
                           ConsultorioRepository consultorioRepository, HorarioDisponibleRepository horarioRepository,
                           Clock clock, PasswordEncoder passwordEncoder, BootstrapProperties bootstrap) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.administrativoRepository = administrativoRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.especialidadRepository = especialidadRepository;
        this.consultorioRepository = consultorioRepository;
        this.horarioRepository = horarioRepository;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.bootstrap = bootstrap;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RolNombre nombre : RolNombre.values()) {
            if (rolRepository.findByNombreRol(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre));
            }
        }
        if (bootstrap.completo() && !usuarioRepository.existsByRolNombreRol(RolNombre.ADMINISTRADOR)) {
            Rol rolAdmin = rolRepository.findByNombreRol(RolNombre.ADMINISTRADOR).orElseThrow();
            Usuario admin = usuarioRepository.save(new Usuario(rolAdmin, bootstrap.adminDocumento().trim(),
                    "Administrador", "General", bootstrap.adminCorreo().trim().toLowerCase(),
                    passwordEncoder.encode(bootstrap.adminPassword())));
            administrativoRepository.save(new Administrativo(admin, "Administrador del sistema"));
            log.info("Administrador inicial creado: {}", admin.getCorreo());
        }
        if (bootstrap.demoActivo()) {
            sincronizarCuentasDemo();
        }
    }

    private void sincronizarCuentasDemo() {
        log.warn("Cuentas demo activas (app.bootstrap.cuentas-demo=true). Desactívelas en producción.");
        sincronizar(RolNombre.ADMINISTRADOR, "11111111", "Admin", "Sistema", "admin@hermilio.com");
        sincronizar(RolNombre.MEDICO, "22222222", "Juan", "Pérez", "medico@hermilio.com");
        sincronizar(RolNombre.RECEPCIONISTA, "33333333", "María", "López", "recepcion@hermilio.com");
        sincronizar(RolNombre.PACIENTE, "44444444", "Carlos", "Gómez", "paciente@hermilio.com");
        sembrarHorariosDemo();
    }

    private void sincronizar(RolNombre rolNombre, String documento, String nombres, String apellidos, String correo) {
        Rol rol = rolRepository.findByNombreRol(rolNombre).orElseThrow();
        Usuario usuario = usuarioRepository.findByCorreoConRol(correo).orElse(null);

        if (usuario == null) {
            if (usuarioRepository.existsByDocumentoIdentidad(documento)) {
                log.warn("Cuenta demo {} omitida: el documento {} ya pertenece a otro usuario", correo, documento);
                return;
            }
            usuario = usuarioRepository.save(new Usuario(rol, documento, nombres, apellidos, correo,
                    passwordEncoder.encode(bootstrap.passwordDemo())));
            log.info("Cuenta demo creada: {} ({})", correo, rolNombre);
        } else {
            boolean cambio = false;
            if (usuario.getPasswordHash() == null || !passwordEncoder.matches(bootstrap.passwordDemo(), usuario.getPasswordHash())) {
                usuario.setPasswordHash(passwordEncoder.encode(bootstrap.passwordDemo()));
                cambio = true;
            }
            if (!usuario.isEstado()) {
                usuario.setEstado(true);
                cambio = true;
            }
            if (usuario.getRol() == null || usuario.getRol().getNombreRol() != rolNombre) {
                usuario.setRol(rol);
                cambio = true;
            }
            if (cambio) {
                usuarioRepository.save(usuario);
                log.info("Cuenta demo reparada (contraseña/estado/rol): {}", correo);
            }
        }
        asegurarPerfil(rolNombre, usuario);
    }

    /**
     * Para poder reservar citas desde el primer arranque: crea dos consultorios y, para el médico demo, franjas de
     * 30 minutos (mañana y tarde) en los próximos 14 días hábiles. Es idempotente: no duplica franjas ya existentes.
     */
    private void sembrarHorariosDemo() {
        var medico = usuarioRepository.findByCorreoConRol("medico@hermilio.com")
                .flatMap(u -> medicoRepository.findByUsuarioId(u.getId())).orElse(null);
        if (medico == null) {
            return;
        }
        if (consultorioRepository.count() == 0) {
            consultorioRepository.save(new Consultorio("Pabellón A", "A-101"));
            consultorioRepository.save(new Consultorio("Pabellón B", "B-201"));
        }
        Consultorio consultorio = consultorioRepository.findAll().get(0);
        LocalTime[] inicios = {LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30),
                LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(14, 0), LocalTime.of(14, 30),
                LocalTime.of(15, 0), LocalTime.of(15, 30)};
        LocalDate dia = LocalDate.now(clock);
        int creados = 0;
        for (int i = 0; i < 14; i++) {
            dia = dia.plusDays(1);
            if (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                i--;
                continue;
            }
            for (LocalTime inicio : inicios) {
                LocalTime fin = inicio.plusMinutes(30);
                if (!horarioRepository.existeSolapeMedico(medico.getId(), dia, inicio, fin, -1L)
                        && !horarioRepository.existeSolapeConsultorio(consultorio.getId(), dia, inicio, fin, -1L)) {
                    horarioRepository.save(new HorarioDisponible(medico, consultorio, dia, inicio, fin,
                            inicio.isBefore(LocalTime.of(13, 0)) ? Turno.MANANA : Turno.TARDE));
                    creados++;
                }
            }
        }
        if (creados > 0) {
            log.info("Horarios demo creados: {} franjas para medico@hermilio.com", creados);
        }
    }

    /** Cada usuario necesita su fila de perfil 1:1 (administrativos, medicos o pacientes). */
    private void asegurarPerfil(RolNombre rolNombre, Usuario usuario) {
        Long id = usuario.getId();
        switch (rolNombre) {
            case ADMINISTRADOR -> {
                if (administrativoRepository.findByUsuarioId(id).isEmpty()) {
                    administrativoRepository.save(new Administrativo(usuario, "Director de Sistemas"));
                }
            }
            case RECEPCIONISTA -> {
                if (administrativoRepository.findByUsuarioId(id).isEmpty()) {
                    administrativoRepository.save(new Administrativo(usuario, "Recepcionista Principal"));
                }
            }
            case PACIENTE -> {
                if (pacienteRepository.findByUsuarioId(id).isEmpty()) {
                    pacienteRepository.save(new Paciente(usuario, "ESSALUD", "987654321", null));
                }
            }
            case MEDICO -> {
                if (medicoRepository.findByUsuarioId(id).isEmpty()) {
                    Especialidad especialidad = especialidadRepository.findAll().stream().findFirst()
                            .orElseGet(() -> especialidadRepository.save(
                                    new Especialidad("Medicina General", "Atención médica primaria y preventiva")));
                    String colegiatura = medicoRepository.existsByNumeroColegiatura("CMP-12345") ? null : "CMP-12345";
                    if (colegiatura != null) {
                        medicoRepository.save(new Medico(usuario, especialidad, colegiatura));
                    } else {
                        log.warn("Perfil de médico demo omitido: la colegiatura CMP-12345 ya existe");
                    }
                }
            }
        }
    }
}
