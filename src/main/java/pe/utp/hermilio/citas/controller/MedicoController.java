package pe.utp.hermilio.citas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.UsuarioDto.MedicoResponse;
import pe.utp.hermilio.citas.service.UsuarioService;

import java.util.List;

/** Catálogo de médicos activos (filtro de agenda en recepción; consulta de disponibilidad en la app). */
@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private final UsuarioService usuarioService;

    public MedicoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** 200. Cualquier usuario autenticado. Filtro opcional ?especialidadId=. */
    @GetMapping
    public List<MedicoResponse> listar(@RequestParam(required = false) Long especialidadId) {
        return usuarioService.listarMedicos(especialidadId);
    }
}
