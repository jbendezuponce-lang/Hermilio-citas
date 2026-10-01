package pe.utp.hermilio.citas.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de una cita. Los nombres siguen el prototipo del documento
 * (Confirmada, Check-in, Reprogramada, Cancelada).
 */
public enum EstadoCita {
    CONFIRMADA, REPROGRAMADA, CHECK_IN, ATENDIDA, CANCELADA, NO_ASISTIO;

    /** Estados que ocupan el horario: cuentan para la validación de duplicados (RF-35). */
    public static final Set<EstadoCita> ACTIVAS = EnumSet.of(CONFIRMADA, REPROGRAMADA, CHECK_IN, ATENDIDA);

    /** Estados en los que la cita puede reprogramarse, cancelarse o registrar check-in. */
    public static final Set<EstadoCita> MODIFICABLES = EnumSet.of(CONFIRMADA, REPROGRAMADA);

    /** Estados que se consideran para calcular el absentismo (excluye canceladas). */
    public static final Set<EstadoCita> CUMPLIDAS_O_PERDIDAS = EnumSet.of(ATENDIDA, CHECK_IN, NO_ASISTIO);
}
