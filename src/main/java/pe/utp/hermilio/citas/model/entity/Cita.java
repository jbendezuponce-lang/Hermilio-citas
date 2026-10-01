package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.utp.hermilio.citas.model.enums.EstadoCita;
import pe.utp.hermilio.citas.model.enums.OrigenCita;

import java.time.LocalDateTime;

/**
 * Clase INTERMEDIA 2: resuelve la relación N:M entre Paciente y HorarioDisponible.
 * Un paciente reserva muchos horarios y, con el historial de cancelaciones y
 * reprogramaciones, un mismo horario puede tener varias citas a lo largo del tiempo.
 * El médico y el consultorio se obtienen a través del horario (sin redundancia).
 */
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario", nullable = false)
    private HorarioDisponible horario;

    @Column(name = "fecha_hora_reserva", nullable = false)
    private LocalDateTime fechaHoraReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 10)
    private OrigenCita origen;

    @Column(name = "reprogramaciones", nullable = false)
    private int reprogramaciones;

    protected Cita() {
    }

    public Cita(Paciente paciente, HorarioDisponible horario, LocalDateTime fechaHoraReserva, EstadoCita estado, OrigenCita origen) {
        this.paciente = paciente;
        this.horario = horario;
        this.fechaHoraReserva = fechaHoraReserva;
        this.estado = estado;
        this.origen = origen;
        this.reprogramaciones = 0;
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public HorarioDisponible getHorario() { return horario; }
    public void setHorario(HorarioDisponible horario) { this.horario = horario; }
    public LocalDateTime getFechaHoraReserva() { return fechaHoraReserva; }
    public EstadoCita getEstado() { return estado; }
    public void setEstado(EstadoCita estado) { this.estado = estado; }
    public OrigenCita getOrigen() { return origen; }
    public int getReprogramaciones() { return reprogramaciones; }
    public void setReprogramaciones(int reprogramaciones) { this.reprogramaciones = reprogramaciones; }
}
