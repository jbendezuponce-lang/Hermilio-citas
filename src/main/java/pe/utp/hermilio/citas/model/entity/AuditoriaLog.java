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

import java.time.LocalDateTime;

/** Trazabilidad de accesos y modificaciones (RNF-14). */
@Entity
@Table(name = "auditoria_logs")
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 300)
    private String accion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected AuditoriaLog() {
    }

    public AuditoriaLog(Usuario usuario, String accion, LocalDateTime fechaHora) {
        this.usuario = usuario;
        this.accion = accion;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
