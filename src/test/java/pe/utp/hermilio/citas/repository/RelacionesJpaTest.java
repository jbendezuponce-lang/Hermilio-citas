package pe.utp.hermilio.citas.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.utp.hermilio.citas.IntegrationTestBase;
import pe.utp.hermilio.citas.model.entity.Cita;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.Especialidad;
import pe.utp.hermilio.citas.model.entity.HistorialClinico;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Paciente;
import pe.utp.hermilio.citas.model.entity.RecetaMedica;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.OrigenCita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Valida el mapeo JPA de las relaciones (incluidas las 3 entidades intermedias) y las consultas críticas. */
class RelacionesJpaTest extends IntegrationTestBase {

    @Autowired private HistorialClinicoRepository historialRepository;
    @Autowired private ListaEsperaRepository listaEsperaRepository;

    @Test
    void horarioIntermedia_unaMedicoYConsultorio_yBusquedaSoloDevuelveLibresDeLaEspecialidad() {
        Especialidad psiquiatria = especialidad("Psiquiatria");
        Especialidad psicologia = especialidad("Psicologia");
        Medico m1 = medico(psiquiatria);
        Medico m2 = medico(psicologia);
        Consultorio c1 = consultorio();
        Consultorio c2 = consultorio();
        HorarioDisponible libre = horario(m1, c1, manana(), LocalTime.of(8, 0));
        HorarioDisponible ocupado = horario(m1, c1, manana(), LocalTime.of(9, 0));
        ocupado.setDisponible(false);
        horario(m2, c2, manana(), LocalTime.of(8, 0));
        horarioRepository.flush();

        List<HorarioDisponible> resultado = horarioRepository.findDisponibles(psiquiatria.getId(), manana(), manana());

        assertEquals(1, resultado.size());
        assertEquals(libre.getId(), resultado.get(0).getId());
        assertEquals(c1.getId(), resultado.get(0).getConsultorio().getId());
    }

    @Test
    void citaIntermedia_unePacienteYHorario_yDetectaDuplicadosSoloDeCitasActivas() {
        Especialidad esp = especialidad("Psiquiatria");
        Medico a = medico(esp);
        Medico b = medico(esp);
        Paciente p = paciente();
        HorarioDisponible h1 = horario(a, consultorio(), manana(), LocalTime.of(10, 0));
        Cita cita = citaRepository.save(new Cita(p, h1, LocalDateTime.now(clock), EstadoCita.CONFIRMADA, OrigenCita.MOVIL));

        boolean duplicada = citaRepository.existeDuplicada(p.getId(), esp.getId(), manana(), LocalTime.of(10, 0),
                EstadoCita.ACTIVAS, -1L);
        assertTrue(duplicada);

        // la propia cita se excluye (caso reprogramación)
        assertFalse(citaRepository.existeDuplicada(p.getId(), esp.getId(), manana(), LocalTime.of(10, 0),
                EstadoCita.ACTIVAS, cita.getId()));

        // una cita cancelada ya no cuenta
        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.flush();
        assertFalse(citaRepository.existeDuplicada(p.getId(), esp.getId(), manana(), LocalTime.of(10, 0),
                EstadoCita.ACTIVAS, -1L));

        // el mismo horario admite varias citas a lo largo del tiempo (relación N:M con historia)
        citaRepository.save(new Cita(paciente(), h1, LocalDateTime.now(clock), EstadoCita.CONFIRMADA, OrigenCita.PRESENCIAL));
        assertEquals(2, citaRepository.findAll().stream().filter(c -> c.getHorario().getId().equals(h1.getId())).count());
        assertTrue(b.getId() != null);
    }

    @Test
    void solapes_medicoYConsultorio() {
        Medico m = medico(especialidad("Psiquiatria"));
        Consultorio c = consultorio();
        HorarioDisponible h = horario(m, c, manana(), LocalTime.of(8, 0)); // 08:00-08:30

        assertTrue(horarioRepository.existeSolapeMedico(m.getId(), manana(), LocalTime.of(8, 15), LocalTime.of(8, 45), -1L));
        assertFalse(horarioRepository.existeSolapeMedico(m.getId(), manana(), LocalTime.of(8, 30), LocalTime.of(9, 0), -1L));
        assertFalse(horarioRepository.existeSolapeMedico(m.getId(), manana(), LocalTime.of(8, 15), LocalTime.of(8, 45), h.getId()));
        assertTrue(horarioRepository.existeSolapeConsultorio(c.getId(), manana(), LocalTime.of(8, 0), LocalTime.of(8, 30), -1L));
    }

    @Test
    void historial_cascadaDeRecetas_yOrphanRemoval() {
        Medico m = medico(especialidad("Psiquiatria"));
        Paciente p = paciente();
        HorarioDisponible h = horario(m, consultorio(), manana(), LocalTime.of(11, 0));
        Cita cita = citaRepository.save(new Cita(p, h, LocalDateTime.now(clock), EstadoCita.ATENDIDA, OrigenCita.MOVIL));

        HistorialClinico hc = new HistorialClinico(cita, "Diagnóstico", "Tratamiento", null, LocalDateTime.now(clock));
        hc.agregarReceta(new RecetaMedica("Sertralina", "50 mg", "Una vez al día"));
        hc.agregarReceta(new RecetaMedica("Clonazepam", "0.5 mg", null));
        historialRepository.saveAndFlush(hc);

        HistorialClinico leido = historialRepository.findDetalleById(hc.getId()).orElseThrow();
        assertEquals(2, leido.getRecetas().size());

        leido.limpiarRecetas();
        leido.agregarReceta(new RecetaMedica("Sertralina", "100 mg", null));
        historialRepository.flush();
        assertEquals(1, historialRepository.findDetalleById(hc.getId()).orElseThrow().getRecetas().size());

        assertEquals(1, historialRepository.findByPaciente(p.getId()).size());
        assertTrue(historialRepository.existsByCitaId(cita.getId()));
    }

    @Test
    void reportes_contarPorEstado_yReprogramadas() {
        Especialidad esp = especialidad("Psiquiatria");
        Medico m = medico(esp);
        Paciente p = paciente();
        LocalDate fecha = manana();
        Cita atendida = citaRepository.save(new Cita(p, horario(m, consultorio(), fecha, LocalTime.of(8, 0)),
                LocalDateTime.now(clock), EstadoCita.ATENDIDA, OrigenCita.MOVIL));
        Cita reprog = citaRepository.save(new Cita(p, horario(m, consultorio(), fecha, LocalTime.of(9, 0)),
                LocalDateTime.now(clock), EstadoCita.NO_ASISTIO, OrigenCita.MOVIL));
        reprog.setReprogramaciones(2);
        citaRepository.flush();

        assertEquals(1, citaRepository.contarReprogramadas(fecha, fecha));
        List<Object[]> filas = citaRepository.inasistenciaPorEspecialidad(fecha, fecha, EstadoCita.NO_ASISTIO,
                EstadoCita.CUMPLIDAS_O_PERDIDAS);
        assertEquals(1, filas.size());
        assertEquals(2L, ((Number) filas.get(0)[2]).longValue());
        assertEquals(1L, ((Number) filas.get(0)[3]).longValue());
        assertTrue(atendida.getId() != null && listaEsperaRepository != null);
    }
}
