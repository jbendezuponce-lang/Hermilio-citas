package pe.utp.hermilio.citas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "consultorios",
        uniqueConstraints = @UniqueConstraint(name = "uk_consultorios_ubicacion", columnNames = {"numero_pabellon", "codigo_room"}))
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consultorio")
    private Long id;

    @Column(name = "numero_pabellon", nullable = false, length = 20)
    private String numeroPabellon;

    @Column(name = "codigo_room", nullable = false, length = 20)
    private String codigoRoom;

    protected Consultorio() {
    }

    public Consultorio(String numeroPabellon, String codigoRoom) {
        this.numeroPabellon = numeroPabellon;
        this.codigoRoom = codigoRoom;
    }

    public Long getId() { return id; }
    public String getNumeroPabellon() { return numeroPabellon; }
    public void setNumeroPabellon(String numeroPabellon) { this.numeroPabellon = numeroPabellon; }
    public String getCodigoRoom() { return codigoRoom; }
    public void setCodigoRoom(String codigoRoom) { this.codigoRoom = codigoRoom; }
}
