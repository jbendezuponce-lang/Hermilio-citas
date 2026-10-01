package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.hermilio.citas.model.entity.Consultorio;

public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {

    boolean existsByNumeroPabellonAndCodigoRoom(String numeroPabellon, String codigoRoom);
}
