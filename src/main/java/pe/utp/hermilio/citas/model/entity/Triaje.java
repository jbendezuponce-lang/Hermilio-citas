package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triajes")
public class Triaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_triaje")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    /** Formato "sistólica/diastólica", p. ej. 120/80. */
    @Column(name = "presion_arterial", length = 10)
    private String presionArterial;

    @Column(name = "peso", precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "signos_vitales", length = 255)
    private String signosVitales;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected Triaje() {
    }

    public Triaje(Cita cita, String presionArterial, BigDecimal peso, String signosVitales, LocalDateTime fechaRegistro) {
        this.cita = cita;
        this.presionArterial = presionArterial;
        this.peso = peso;
        this.signosVitales = signosVitales;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() { return id; }
    public Cita getCita() { return cita; }
    public String getPresionArterial() { return presionArterial; }
    public void setPresionArterial(String presionArterial) { this.presionArterial = presionArterial; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    public String getSignosVitales() { return signosVitales; }
    public void setSignosVitales(String signosVitales) { this.signosVitales = signosVitales; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
