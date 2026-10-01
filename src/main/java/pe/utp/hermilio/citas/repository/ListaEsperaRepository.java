package pe.utp.hermilio.citas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.hermilio.citas.model.entity.ListaEspera;
import pe.utp.hermilio.citas.model.enums.EstadoListaEspera;

import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    String FETCH = "select l from ListaEspera l join fetch l.paciente p join fetch p.usuario u join fetch l.especialidad e ";

    @Query(FETCH + "where l.id = :id")
    Optional<ListaEspera> findDetalleById(@Param("id") Long id);

    @Query(FETCH + "where l.estado = :estado order by l.fechaRegistro")
    List<ListaEspera> findByEstado(@Param("estado") EstadoListaEspera estado);

    @Query(FETCH + "where l.estado = :estado and e.id = :especialidadId order by l.fechaRegistro")
    List<ListaEspera> findByEstadoYEspecialidad(@Param("estado") EstadoListaEspera estado,
                                                @Param("especialidadId") Long especialidadId);

    boolean existsByPacienteIdAndEspecialidadIdAndEstado(Long pacienteId, Long especialidadId, EstadoListaEspera estado);
}
