package pe.utp.hermilio.citas.dto;

import java.time.Instant;
import java.util.List;

/** Formato único de error para toda la API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> detalles) {

    public static ApiError of(int status, String error, String message, String path, List<String> detalles) {
        return new ApiError(Instant.now(), status, error, message, path, detalles);
    }
}
