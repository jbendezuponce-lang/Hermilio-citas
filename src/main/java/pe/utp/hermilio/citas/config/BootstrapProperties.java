package pe.utp.hermilio.citas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de arranque (app.bootstrap.*).
 * <ul>
 *   <li>adminDocumento / adminCorreo / adminPassword: primer administrador (RF-31). Vacíos = no se crea nada.</li>
 *   <li>cuentasDemo: si es true, al arrancar se garantizan 4 cuentas de prueba (una por rol) y se corrige su
 *       contraseña si no coincide con passwordDemo. Pensado para desarrollo local; en producción debe ser false.</li>
 *   <li>passwordDemo: contraseña en texto plano de las cuentas demo (se guarda siempre cifrada con BCrypt).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(String adminDocumento, String adminCorreo, String adminPassword,
                                  boolean cuentasDemo, String passwordDemo) {

    public boolean completo() {
        return notBlank(adminDocumento) && notBlank(adminCorreo) && notBlank(adminPassword);
    }

    public boolean demoActivo() {
        return cuentasDemo && notBlank(passwordDemo);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
