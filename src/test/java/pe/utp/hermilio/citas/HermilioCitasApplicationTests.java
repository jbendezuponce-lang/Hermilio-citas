package pe.utp.hermilio.citas;

import org.junit.jupiter.api.Test;
import pe.utp.hermilio.citas.model.enums.RolNombre;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** El contexto arranca (entidades, repositorios, seguridad, scheduler) y los 4 roles quedan inicializados. */
class HermilioCitasApplicationTests extends IntegrationTestBase {

    @Test
    void contextoArranca_yRolesInicializados() {
        for (RolNombre nombre : RolNombre.values()) {
            assertTrue(rolRepository.findByNombreRol(nombre).isPresent(), "Falta el rol " + nombre);
        }
    }
}
