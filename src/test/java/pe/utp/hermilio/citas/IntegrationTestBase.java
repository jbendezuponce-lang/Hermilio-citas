package pe.utp.hermilio.citas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
import pe.utp.hermilio.citas.repository.CitaRepository;
import pe.utp.hermilio.citas.repository.ConsultorioRepository;
import pe.utp.hermilio.citas.repository.EspecialidadRepository;
import pe.utp.hermilio.citas.repository.HorarioDisponibleRepository;
import pe.utp.hermilio.citas.repository.MedicoRepository;
import pe.utp.hermilio.citas.repository.PacienteRepository;
import pe.utp.hermilio.citas.repository.RolRepository;
import pe.utp.hermilio.citas.repository.UsuarioRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de las pruebas de integración: contexto completo de Spring sobre H2 (modo MySQL) y una transacción
 * por prueba que se revierte al terminar. Aporta fábricas de datos para no repetir código.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Clave12345";
    private static final AtomicInteger SECUENCIA = new AtomicInteger(10_000_000);

    @Autowired protected RolRepository rolRepository;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected PacienteRepository pacienteRepository;
    @Autowired protected MedicoRepository medicoRepository;
    @Autowired protected AdministrativoRepository administrativoRepository;
    @Autowired protected EspecialidadRepository especialidadRepository;
    @Autowired protected ConsultorioRepository consultorioRepository;
    @Autowired protected HorarioDisponibleRepository horarioRepository;
    @Autowired protected CitaRepository citaRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected Clock clock;

    protected Rol rol(RolNombre nombre) {
        return rolRepository.findByNombreRol(nombre).orElseGet(() -> rolRepository.save(new Rol(nombre)));
    }

    protected Usuario usuario(RolNombre rol) {
        int n = SECUENCIA.incrementAndGet();
        return usuarioRepository.save(new Usuario(rol(rol), String.valueOf(n), "Nombre" + n, "Apellido" + n,
                "u" + n + "@hospital.test", passwordEncoder.encode(PASSWORD)));
    }

    protected Paciente paciente() {
        return pacienteRepository.save(new Paciente(usuario(RolNombre.PACIENTE), "SIS", "999888777", null));
    }

    protected Especialidad especialidad(String nombre) {
        return especialidadRepository.save(new Especialidad(nombre + SECUENCIA.incrementAndGet(), null));
    }

    protected Medico medico(Especialidad especialidad) {
        Usuario u = usuario(RolNombre.MEDICO);
        return medicoRepository.save(new Medico(u, especialidad, "CMP" + u.getId()));
    }

    protected Consultorio consultorio() {
        int n = SECUENCIA.incrementAndGet();
        return consultorioRepository.save(new Consultorio("P" + n, "R" + n));
    }

    protected HorarioDisponible horario(Medico medico, Consultorio consultorio, LocalDate fecha, LocalTime inicio) {
        Turno turno = inicio.isBefore(LocalTime.of(13, 0)) ? Turno.MANANA : Turno.TARDE;
        return horarioRepository.save(new HorarioDisponible(medico, consultorio, fecha, inicio, inicio.plusMinutes(30), turno));
    }

    protected LocalDate manana() {
        return LocalDate.now(clock).plusDays(1);
    }

    protected UsuarioDetails actor(Usuario usuario) {
        return UsuarioDetails.de(usuario);
    }
}
