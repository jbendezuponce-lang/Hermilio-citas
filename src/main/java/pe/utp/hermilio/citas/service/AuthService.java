package pe.utp.hermilio.citas.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.hermilio.citas.dto.AuthDto.LoginRequest;
import pe.utp.hermilio.citas.dto.AuthDto.LoginResponse;
import pe.utp.hermilio.citas.exception.BusinessRuleException;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.repository.UsuarioRepository;
import pe.utp.hermilio.citas.security.JwtService;
import pe.utp.hermilio.citas.security.TokenBlacklist;
import pe.utp.hermilio.citas.security.UsuarioDetails;

/** RF-02, RF-03, RF-15, RF-16, RF-26: autenticación, cierre de sesión y recuperación de contraseña. */
@Service
public class AuthService {

    private static final String TOKEN_RESET_INVALIDO = "El token de recuperación es inválido o expiró";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final PasswordEncoder passwordEncoder;
    private final NotificadorCorreo notificadorCorreo;
    private final AuditoriaService auditoriaService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                       JwtService jwtService, TokenBlacklist tokenBlacklist, PasswordEncoder passwordEncoder,
                       NotificadorCorreo notificadorCorreo, AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.passwordEncoder = passwordEncoder;
        this.notificadorCorreo = notificadorCorreo;
        this.auditoriaService = auditoriaService;
    }

    /** Sin @Transactional a propósito: el registro de un intento fallido debe persistir aunque se relance la excepción. */
    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim();
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, request.password()));
        } catch (AuthenticationException ex) {
            usuarioRepository.findByCorreoConRol(correo)
                    .ifPresent(u -> auditoriaService.registrar(u.getId(), "LOGIN_FALLIDO"));
            throw ex;
        }
        UsuarioDetails detalles = (UsuarioDetails) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByCorreoConRol(detalles.getUsername()).orElseThrow();
        auditoriaService.registrar(usuario.getId(), "LOGIN");
        return new LoginResponse(jwtService.generarTokenAcceso(detalles), "Bearer", jwtService.segundosDeVigencia(),
                usuario.getId(), usuario.getNombres(), usuario.getApellidos(), detalles.getRol());
    }

    /** RF-15: invalida el token en uso; el cliente además descarta su copia. */
    public void logout(UsuarioDetails actor, String token) {
        try {
            Claims claims = jwtService.parsear(token);
            tokenBlacklist.revocar(claims.getId(), claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            // token ya inválido o expirado: no hay nada que revocar
        }
        auditoriaService.registrar(actor.getId(), "LOGOUT");
    }

    /** RF-03, paso 1. Responde igual exista o no el correo para no revelar qué cuentas están registradas. */
    @Transactional(readOnly = true)
    public void solicitarRecuperacion(String correo) {
        usuarioRepository.findByCorreoConRol(correo.trim())
                .filter(Usuario::isEstado)
                .ifPresent(u -> notificadorCorreo.enviarRecuperacion(u.getCorreo(), jwtService.generarTokenReset(u)));
    }

    /** RF-03, paso 2. */
    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        Claims claims;
        try {
            claims = jwtService.parsear(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessRuleException(TOKEN_RESET_INVALIDO);
        }
        if (!JwtService.TIPO_RESET.equals(jwtService.tipo(claims))) {
            throw new BusinessRuleException(TOKEN_RESET_INVALIDO);
        }
        Usuario usuario = usuarioRepository.findByCorreoConRol(claims.getSubject())
                .orElseThrow(() -> new BusinessRuleException(TOKEN_RESET_INVALIDO));
        if (!usuario.isEstado() || !jwtService.huellaCoincide(claims, usuario.getPasswordHash())) {
            throw new BusinessRuleException(TOKEN_RESET_INVALIDO);
        }
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        auditoriaService.registrar(usuario.getId(), "PASSWORD_RESTABLECIDA");
    }
}
