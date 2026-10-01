package pe.utp.hermilio.citas.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utp.hermilio.citas.dto.AuthDto.LoginRequest;
import pe.utp.hermilio.citas.dto.AuthDto.LoginResponse;
import pe.utp.hermilio.citas.dto.AuthDto.MensajeResponse;
import pe.utp.hermilio.citas.dto.AuthDto.RecuperarPasswordRequest;
import pe.utp.hermilio.citas.dto.AuthDto.RestablecerPasswordRequest;
import pe.utp.hermilio.citas.dto.PacienteDto.PacienteResponse;
import pe.utp.hermilio.citas.dto.PacienteDto.RegistroPacienteRequest;
import pe.utp.hermilio.citas.security.UsuarioDetails;
import pe.utp.hermilio.citas.service.AuthService;
import pe.utp.hermilio.citas.service.PacienteService;

/** RF-01, RF-02, RF-03, RF-15, RF-16, RF-26. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PacienteService pacienteService;

    public AuthController(AuthService authService, PacienteService pacienteService) {
        this.authService = authService;
        this.pacienteService = pacienteService;
    }

    /** RF-02 / RF-16 / RF-26: inicio de sesión de cualquier perfil. 200 | 400 | 401. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** RF-01: auto-registro del paciente o apoderado. 201 | 400 | 409. */
    @PostMapping("/registro")
    public ResponseEntity<PacienteResponse> registro(@Valid @RequestBody RegistroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.registrar(request, true, null));
    }

    /** RF-15: cierre de sesión seguro (invalida el token). 204 | 401. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioDetails actor,
                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(actor, authorization.replaceFirst("(?i)^Bearer\\s+", "").trim());
        return ResponseEntity.noContent().build();
    }

    /** RF-03 (paso 1): siempre 202 con el mismo mensaje. */
    @PostMapping("/recuperar-password")
    public ResponseEntity<MensajeResponse> recuperar(@Valid @RequestBody RecuperarPasswordRequest request) {
        authService.solicitarRecuperacion(request.correo());
        return ResponseEntity.accepted().body(new MensajeResponse(
                "Si el correo está registrado, recibirá las instrucciones para restablecer su contraseña"));
    }

    /** RF-03 (paso 2). 200 | 400. */
    @PostMapping("/restablecer-password")
    public ResponseEntity<MensajeResponse> restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request.token(), request.nuevaPassword());
        return ResponseEntity.ok(new MensajeResponse("Contraseña actualizada correctamente"));
    }
}
