package pe.utp.hermilio.citas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.utp.hermilio.citas.config.JwtProperties;
import pe.utp.hermilio.citas.model.entity.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/** Emisión y validación de tokens JWT (acceso y restablecimiento de contraseña). */
@Service
public class JwtService {

    public static final String TIPO_ACCESO = "access";
    public static final String TIPO_RESET = "reset";
    private static final String CLAIM_TIPO = "typ";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_HUELLA = "fp";

    private final SecretKey key;
    private final JwtProperties props;
    private final Clock clock;

    public JwtService(JwtProperties props, Clock clock) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("La variable JWT_SECRET (app.jwt.secret) debe tener al menos 32 caracteres");
        }
        this.props = props;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generarTokenAcceso(UsuarioDetails u) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(u.getUsername())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TIPO, TIPO_ACCESO)
                .claim(CLAIM_ROL, u.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofMinutes(props.expirationMinutes()))))
                .signWith(key)
                .compact();
    }

    public long segundosDeVigencia() {
        return Duration.ofMinutes(props.expirationMinutes()).toSeconds();
    }

    /**
     * Token de restablecimiento: incluye una huella del hash de contraseña actual, por lo que deja de ser válido
     * apenas la contraseña cambia (uso único).
     */
    public String generarTokenReset(Usuario u) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(u.getCorreo())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TIPO, TIPO_RESET)
                .claim(CLAIM_HUELLA, huella(u.getPasswordHash()))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofMinutes(props.resetExpirationMinutes()))))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException si el token es inválido, alterado o expirado. */
    public Claims parsear(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String tipo(Claims claims) {
        return claims.get(CLAIM_TIPO, String.class);
    }

    public boolean huellaCoincide(Claims claims, String passwordHash) {
        return huella(passwordHash).equals(claims.get(CLAIM_HUELLA, String.class));
    }

    private String huella(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
