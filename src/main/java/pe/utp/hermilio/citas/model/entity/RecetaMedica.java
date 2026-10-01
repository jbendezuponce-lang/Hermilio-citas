package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recetas_medicas")
public class RecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_receta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historial", nullable = false)
    private HistorialClinico historial;

    @Column(name = "medicamento", nullable = false, length = 150)
    private String medicamento;

    @Column(name = "dosis", nullable = false, length = 100)
    private String dosis;

    @Column(name = "indicaciones", length = 500)
    private String indicaciones;

    protected RecetaMedica() {
    }

    public RecetaMedica(String medicamento, String dosis, String indicaciones) {
        this.medicamento = medicamento;
        this.dosis = dosis;
        this.indicaciones = indicaciones;
    }

    public Long getId() { return id; }
    public HistorialClinico getHistorial() { return historial; }
    void setHistorial(HistorialClinico historial) { this.historial = historial; }
    public String getMedicamento() { return medicamento; }
    public String getDosis() { return dosis; }
    public String getIndicaciones() { return indicaciones; }
}
