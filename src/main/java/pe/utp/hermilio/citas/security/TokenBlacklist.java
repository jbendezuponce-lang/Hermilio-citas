package pe.utp.hermilio.citas.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista en memoria de tokens cerrados (RF-15: cierre de sesión seguro).
 * Limitación conocida: con varias instancias del backend habría que compartirla (p. ej. Redis).
 */
@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Instant> revocados = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void revocar(String jti, Instant expiraEn) {
        purgar();
        revocados.put(jti, expiraEn);
    }

    public boolean estaRevocado(String jti) {
        return jti != null && revocados.containsKey(jti);
    }

    private void purgar() {
        Instant ahora = clock.instant();
        revocados.entrySet().removeIf(e -> e.getValue().isBefore(ahora));
    }
}
