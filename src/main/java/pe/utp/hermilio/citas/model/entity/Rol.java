package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.utp.hermilio.citas.model.enums.RolNombre;

@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 20)
    private RolNombre nombreRol;

    protected Rol() {
    }

    public Rol(RolNombre nombreRol) {
        this.nombreRol = nombreRol;
    }

    public Long getId() { return id; }
    public RolNombre getNombreRol() { return nombreRol; }
    public void setNombreRol(RolNombre nombreRol) { this.nombreRol = nombreRol; }
}
