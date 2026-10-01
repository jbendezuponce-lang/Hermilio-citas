package pe.utp.hermilio.citas.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.AuditoriaLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    @Query("select a from AuditoriaLog a left join fetch a.usuario u "
            + "where a.fechaHora between :desde and :hasta order by a.fechaHora desc")
    List<AuditoriaLog> findEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                 Pageable pageable);
}
