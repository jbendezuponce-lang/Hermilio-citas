package pe.utp.hermilio.citas.exception;

/** 400: la petición es válida sintácticamente pero incumple una regla de negocio. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
