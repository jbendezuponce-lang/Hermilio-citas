package pe.utp.hermilio.citas.exception;

/** 403: el usuario autenticado no puede operar sobre este recurso concreto (p. ej. la cita de otro paciente). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
