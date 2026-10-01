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

@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "tipo_seguro", length = 50)
    private String tipoSeguro;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "datos_apoderado", length = 255)
    private String datosApoderado;

    protected Paciente() {
    }

    public Paciente(Usuario usuario, String tipoSeguro, String telefonoContacto, String datosApoderado) {
        this.usuario = usuario;
        this.tipoSeguro = tipoSeguro;
        this.telefonoContacto = telefonoContacto;
        this.datosApoderado = datosApoderado;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(String tipoSeguro) { this.tipoSeguro = tipoSeguro; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }
    public String getDatosApoderado() { return datosApoderado; }
    public void setDatosApoderado(String datosApoderado) { this.datosApoderado = datosApoderado; }
}
