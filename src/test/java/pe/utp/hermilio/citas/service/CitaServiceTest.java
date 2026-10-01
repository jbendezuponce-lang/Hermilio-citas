package pe.utp.hermilio.citas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.utp.hermilio.citas.IntegrationTestBase;
import pe.utp.hermilio.citas.dto.CitaDto.CitaResponse;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.exception.ConflictException;
import pe.utp.hermilio.citas.exception.ForbiddenOperationException;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.OrigenCita;
import pe.utp.hermilio.citas.model.enums.RolNombre;
import pe.utp.hermilio.citas.repository.NotificacionRepository;
import pe.utp.hermilio.citas.security.UsuarioDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas de negocio de las citas: RF-07/08/09, RF-20 a RF-23, RF-30, RF-35 y RN de propiedad por rol. */
class CitaServiceTest extends IntegrationTestBase {

    @Autowired CitaService citaService;
    @Autowired NotificacionRepository notificacionRepository;

    Especialidad especialidad;
    Medico medico;
    Consultorio consultorio;
    Paciente paciente;
    UsuarioDetails actorPaciente;

    @BeforeEach
    void datos() {
        especialidad = especialidad("Psiquiatria");
        medico = medico(especialidad);
        consultorio = consultorio();
        paciente = paciente();
        actorPaciente = actor(paciente.getUsuario());
    }

    private HorarioDisponible horarioManana(int hora) {
        return horario(medico, consultorio, manana(), LocalTime.of(hora, 0));
    }

    /** Cita creada directamente (sin pasar por reservar) para poder situarla en el día de hoy. */
    private Cita citaHoy(EstadoCita estado, OrigenCita origen) {
        HorarioDisponible h = horario(medico, consultorio, LocalDate.now(clock), LocalTime.of(23, 0));
        h.setDisponible(false);
        return citaRepository.save(new Cita(paciente, h, LocalDateTime.now(clock), estado, origen));
    }

    @Test
    void reservarOcupaElHorarioYCreaNotificacionDeConfirmacion() {
        HorarioDisponible h = horarioManana(9);
        CitaResponse r = citaService.reservar(actorPaciente, h.getId());

        assertEquals(EstadoCita.CONFIRMADA, r.estado());
        assertEquals(OrigenCita.MOVIL, r.origen());
        assertEquals(consultorio.getNumeroPabellon(), r.numeroPabellon()); // RF-14
        assertFalse(horarioRepository.findById(h.getId()).orElseThrow().isDisponible());
        assertEquals(1, notificacionRepository.findByPaciente(paciente.getId()).size());
    }

    @Test
    void reservarHorarioOcupadoLanzaConflicto() {
        HorarioDisponible h = horarioManana(9);
        citaService.reservar(actorPaciente, h.getId());
        UsuarioDetails otro = actor(paciente().getUsuario());
        assertThrows(ConflictException.class, () -> citaService.reservar(otro, h.getId()));
    }

    @Test
    void reservarMismaEspecialidadFechaYHoraEsDuplicado_RF35() {
        Medico otroMedico = medico(especialidad);
        HorarioDisponible h1 = horarioManana(9);
        HorarioDisponible h2 = horario(otroMedico, consultorio(), manana(), LocalTime.of(9, 0));
        citaService.reservar(actorPaciente, h1.getId());
        assertThrows(ConflictException.class, () -> citaService.reservar(actorPaciente, h2.getId()));
    }

    @Test
    void reservarHorarioPasadoEsReglaDeNegocio() {
        HorarioDisponible pasado = horario(medico, consultorio, LocalDate.now(clock).minusDays(1), LocalTime.of(9, 0));
        assertThrows(BusinessRuleException.class, () -> citaService.reservar(actorPaciente, pasado.getId()));
    }

    @Test
    void cancelarLiberaElHorario() {
        HorarioDisponible h = horarioManana(9);
        CitaResponse r = citaService.reservar(actorPaciente, h.getId());
        CitaResponse cancelada = citaService.cancelar(actorPaciente, r.idCita());

        assertEquals(EstadoCita.CANCELADA, cancelada.estado());
        assertTrue(horarioRepository.findById(h.getId()).orElseThrow().isDisponible());
        assertThrows(ConflictException.class, () -> citaService.cancelar(actorPaciente, r.idCita()));
    }

    @Test
    void reprogramarCambiaHorarioIncrementaContadorYLiberaElAnterior() {
        HorarioDisponible h1 = horarioManana(9);
        HorarioDisponible h2 = horarioManana(10);
        CitaResponse r = citaService.reservar(actorPaciente, h1.getId());
        CitaResponse rep = citaService.reprogramar(actorPaciente, r.idCita(), h2.getId());

        assertEquals(EstadoCita.REPROGRAMADA, rep.estado());
        assertEquals(1, rep.reprogramaciones());
        assertEquals(h2.getId(), rep.idHorario());
        assertTrue(horarioRepository.findById(h1.getId()).orElseThrow().isDisponible());
        assertFalse(horarioRepository.findById(h2.getId()).orElseThrow().isDisponible());
    }

    @Test
    void reprogramarAOtraEspecialidadEsRechazado() {
        Medico otraEsp = medico(especialidad("Psicologia"));
        HorarioDisponible h1 = horarioManana(9);
        HorarioDisponible h2 = horario(otraEsp, consultorio(), manana(), LocalTime.of(11, 0));
        CitaResponse r = citaService.reservar(actorPaciente, h1.getId());
        assertThrows(BusinessRuleException.class, () -> citaService.reprogramar(actorPaciente, r.idCita(), h2.getId()));
    }

    @Test
    void pacienteNoPuedeOperarCitaDeOtroPaciente() {
        CitaResponse r = citaService.reservar(actorPaciente, horarioManana(9).getId());
        UsuarioDetails intruso = actor(paciente().getUsuario());
        assertThrows(ForbiddenOperationException.class, () -> citaService.cancelar(intruso, r.idCita()));
        assertThrows(ForbiddenOperationException.class, () -> citaService.obtener(intruso, r.idCita()));
    }

    @Test
    void recepcionSoloGestionaCitasPresenciales_RF21_RF22() {
        UsuarioDetails recepcion = actor(usuario(RolNombre.RECEPCIONISTA));
        CitaResponse movil = citaService.reservar(actorPaciente, horarioManana(9).getId());
        assertThrows(ForbiddenOperationException.class, () -> citaService.cancelar(recepcion, movil.idCita()));

        Paciente otro = paciente();
        CitaResponse presencial = citaService.reservarPresencial(recepcion, otro.getId(), horarioManana(10).getId());
        assertEquals(OrigenCita.PRESENCIAL, presencial.origen());
        assertEquals(EstadoCita.CANCELADA, citaService.cancelar(recepcion, presencial.idCita()).estado());
    }

    @Test
    void checkInSoloElDiaDeLaCita_RF23() {
        UsuarioDetails recepcion = actor(usuario(RolNombre.RECEPCIONISTA));
        CitaResponse futura = citaService.reservar(actorPaciente, horarioManana(9).getId());
        assertThrows(ConflictException.class, () -> citaService.registrarCheckIn(recepcion, futura.idCita()));

        Cita hoy = citaHoy(EstadoCita.CONFIRMADA, OrigenCita.PRESENCIAL);
        assertEquals(EstadoCita.CHECK_IN, citaService.registrarCheckIn(recepcion, hoy.getId()).estado());
    }

    @Test
    void atenderExigeCheckInYSerElMedicoDeLaCita_RF30() {
        Cita hoy = citaHoy(EstadoCita.CONFIRMADA, OrigenCita.PRESENCIAL);
        UsuarioDetails elMedico = actor(medico.getUsuario());
        UsuarioDetails otroMedico = actor(medico(especialidad).getUsuario());

        assertThrows(ConflictException.class, () -> citaService.marcarAtendida(elMedico, hoy.getId()));
        hoy.setEstado(EstadoCita.CHECK_IN);
        assertThrows(ForbiddenOperationException.class, () -> citaService.marcarAtendida(otroMedico, hoy.getId()));
        assertEquals(EstadoCita.ATENDIDA, citaService.marcarAtendida(elMedico, hoy.getId()).estado());
    }

    @Test
    void noAsistioNoAplicaACitasFuturasYMantieneElHorarioOcupado() {
        UsuarioDetails recepcion = actor(usuario(RolNombre.RECEPCIONISTA));
        CitaResponse futura = citaService.reservar(actorPaciente, horarioManana(9).getId());
        assertThrows(ConflictException.class, () -> citaService.marcarNoAsistio(recepcion, futura.idCita()));

        Cita hoy = citaHoy(EstadoCita.CONFIRMADA, OrigenCita.MOVIL);
        assertEquals(EstadoCita.NO_ASISTIO, citaService.marcarNoAsistio(recepcion, hoy.getId()).estado());
        assertFalse(hoy.getHorario().isDisponible());
    }

    @Test
    void misCitasSoloDevuelveLasDelPaciente_RF10() {
        citaService.reservar(actorPaciente, horarioManana(9).getId());
        UsuarioDetails otro = actor(paciente().getUsuario());
        citaService.reservar(otro, horarioManana(10).getId());
        assertEquals(1, citaService.misCitas(actorPaciente).size());
    }
}
