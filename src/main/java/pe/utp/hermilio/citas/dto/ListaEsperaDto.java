package pe.utp.hermilio.citas.dto;

import jakarta.validation.constraints.NotNull;
import pe.utp.hermilio.citas.model.enums.EstadoListaEspera;

import java.time.LocalDateTime;

public final class ListaEsperaDto {

    private ListaEsperaDto() {
    }

    public record AgregarListaEsperaRequest(
            @NotNull(message = "El paciente es obligatorio") Long idPaciente,
            @NotNull(message = "La especialidad es obligatoria") Long idEspecialidad) {
    }

    public record ListaEsperaResponse(Long idListaEspera, Long idPaciente, String paciente, String documentoPaciente,
                                      Long idEspecialidad, String especialidad,
                                      LocalDateTime fechaRegistro, EstadoListaEspera estado) {
    }
}
