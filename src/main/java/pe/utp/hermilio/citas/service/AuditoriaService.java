package pe.utp.hermilio.citas.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.DtoMapper;
import pe.utp.hermilio.citas.dto.ReporteDto.AuditoriaResponse;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.model.entity.AuditoriaLog;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.repository.AuditoriaLogRepository;
import pe.utp.hermilio.citas.repository.UsuarioRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** RNF-14: trazabilidad de accesos y modificaciones (sobre todo del historial clínico electrónico). */
@Service
public class AuditoriaService {

    private static final int MAX_ACCION = 300;
    private static final int MAX_RESULTADOS = 500;

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public AuditoriaService(AuditoriaLogRepository auditoriaLogRepository, UsuarioRepository usuarioRepository, Clock clock) {
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(Long usuarioId, String accion) {
        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
        String texto = accion.length() > MAX_ACCION ? accion.substring(0, MAX_ACCION) : accion;
        auditoriaLogRepository.save(new AuditoriaLog(usuario, texto, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(LocalDate desde, LocalDate hasta, Long usuarioId) {
        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return auditoriaLogRepository
                .findEntre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay().minusNanos(1), PageRequest.of(0, MAX_RESULTADOS))
                .stream()
                .filter(l -> usuarioId == null || (l.getUsuario() != null && usuarioId.equals(l.getUsuario().getId())))
                .map(DtoMapper::auditoria)
                .toList();
    }
}
