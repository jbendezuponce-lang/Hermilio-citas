package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro clínico de una consulta (una cita atendida genera a lo sumo un registro). */
@Entity
@Table(name = "historiales_clinicos")
public class HistorialClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "tratamiento", nullable = false, columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /** Composición: las recetas no existen sin su historial, por eso cascada y orphanRemoval. */
    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecetaMedica> recetas = new ArrayList<>();

    protected HistorialClinico() {
    }

    public HistorialClinico(Cita cita, String diagnostico, String tratamiento, String observaciones, LocalDateTime fechaRegistro) {
        this.cita = cita;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.observaciones = observaciones;
        this.fechaRegistro = fechaRegistro;
    }

    public void agregarReceta(RecetaMedica receta) {
        receta.setHistorial(this);
        this.recetas.add(receta);
    }

    public void limpiarRecetas() {
        this.recetas.clear();
    }

    public Long getId() { return id; }
    public Cita getCita() { return cita; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public List<RecetaMedica> getRecetas() { return recetas; }
}
