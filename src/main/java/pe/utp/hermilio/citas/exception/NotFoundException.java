package pe.utp.hermilio.citas.exception;

/** 404: el recurso solicitado no existe. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
