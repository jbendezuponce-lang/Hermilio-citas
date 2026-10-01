package pe.utp.hermilio.citas.exception;

/** 409: la operación choca con el estado actual (cita duplicada, horario ocupado, dato único repetido...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
