package pe.utp.hermilio.citas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades app.jwt.* (el secreto llega por variable de entorno JWT_SECRET). */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes, long resetExpirationMinutes) {
}
