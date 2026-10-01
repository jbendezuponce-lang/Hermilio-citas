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
import pe.utp.hermilio.citas.model.enums.EstadoListaEspera;

import java.time.LocalDateTime;

/**
 * Clase INTERMEDIA 3 (RF-24): resuelve la relación N:M entre Paciente y Especialidad.
 * Un paciente puede esperar cupo en varias especialidades y una especialidad tiene
 * muchos pacientes en espera.
 */
@Entity
@Table(name = "lista_espera")
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lista_espera")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoListaEspera estado;

    protected ListaEspera() {
    }

    public ListaEspera(Paciente paciente, Especialidad especialidad, LocalDateTime fechaRegistro) {
        this.paciente = paciente;
        this.especialidad = especialidad;
        this.fechaRegistro = fechaRegistro;
        this.estado = EstadoListaEspera.EN_ESPERA;
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public Especialidad getEspecialidad() { return especialidad; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public EstadoListaEspera getEstado() { return estado; }
    public void setEstado(EstadoListaEspera estado) { this.estado = estado; }
}
