package pe.utp.hermilio.citas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    /** Reloj único de la aplicación (zona horaria del hospital); facilita pruebas y evita depender de la zona del servidor. */
    @Bean
    public Clock clock(@Value("${app.timezone:America/Lima}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
