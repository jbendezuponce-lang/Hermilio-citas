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
import jakarta.persistence.UniqueConstraint;
import pe.utp.hermilio.citas.model.enums.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Clase INTERMEDIA 1: resuelve la relación N:M entre Medico y Consultorio.
 * Un médico atiende en distintos consultorios según la fecha/hora y un consultorio
 * es usado por varios médicos; cada franja concreta es un HorarioDisponible.
 */
@Entity
@Table(name = "horarios_disponibles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_horario_medico", columnNames = {"id_medico", "fecha", "hora_inicio"}),
                @UniqueConstraint(name = "uk_horario_consultorio", columnNames = {"id_consultorio", "fecha", "hora_inicio"})
        })
public class HorarioDisponible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_medico", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_consultorio", nullable = false)
    private Consultorio consultorio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno", nullable = false, length = 10)
    private Turno turno;

    @Column(name = "disponible", nullable = false)
    private boolean disponible = true;

    protected HorarioDisponible() {
    }

    public HorarioDisponible(Medico medico, Consultorio consultorio, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Turno turno) {
        this.medico = medico;
        this.consultorio = consultorio;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.turno = turno;
    }

    public Long getId() { return id; }
    public Medico getMedico() { return medico; }
    public Consultorio getConsultorio() { return consultorio; }
    public void setConsultorio(Consultorio consultorio) { this.consultorio = consultorio; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}
