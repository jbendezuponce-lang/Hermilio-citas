package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.hermilio.citas.model.entity.Triaje;

import java.util.Optional;

public interface TriajeRepository extends JpaRepository<Triaje, Long> {

    Optional<Triaje> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);
}
