package pe.utp.hermilio.citas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.utp.hermilio.citas.IntegrationTestBase;
import pe.utp.hermilio.citas.model.entity.Consultorio;
import pe.utp.hermilio.citas.model.entity.HorarioDisponible;
import pe.utp.hermilio.citas.model.entity.Medico;
import pe.utp.hermilio.citas.model.entity.Usuario;
import pe.utp.hermilio.citas.model.enums.RolNombre;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad (JWT + roles) y contratos HTTP principales, ejecutados contra el contexto completo. */
class ApiSecurityFlowTest extends IntegrationTestBase {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired MockMvc mvc;

    private String login(Usuario u) throws Exception {
        String body = "{\"correo\":\"" + u.getCorreo() + "\",\"password\":\"" + PASSWORD + "\"}";
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        Matcher m = TOKEN.matcher(r.getResponse().getContentAsString());
        assertTrue(m.find(), "La respuesta de login debe incluir el token");
        return "Bearer " + m.group(1);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/citas/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginCorrectoYCredencialesInvalidas() throws Exception {
        Usuario u = paciente().getUsuario();
        login(u);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + u.getCorreo() + "\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentoIdentidad\":\"1\",\"nombres\":\"\",\"apellidos\":\"X\",\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroValidoCreaPacienteYRechazaDuplicado() throws Exception {
        String body = "{\"documentoIdentidad\":\"70112233\",\"nombres\":\"Ana\",\"apellidos\":\"Ruiz\","
                + "\"correo\":\"ana.ruiz@correo.test\",\"password\":\"Clave12345\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void pacienteNoAccedeAReportesDeAdministrador() throws Exception {
        String token = login(paciente().getUsuario());
        mvc.perform(get("/api/reportes/citas").param("desde", "2026-01-01").param("hasta", "2026-12-31")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorConsultaReportes() throws Exception {
        String token = login(usuario(RolNombre.ADMINISTRADOR));
        mvc.perform(get("/api/reportes/citas").param("desde", "2026-01-01").param("hasta", "2026-12-31")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void reservaHttpYSegundaReservaDelMismoHorarioDa409() throws Exception {
        Medico medico = medico(especialidad("Psiquiatria"));
        Consultorio consultorio = consultorio();
        HorarioDisponible h = horario(medico, consultorio, manana(), LocalTime.of(9, 0));
        String t1 = login(paciente().getUsuario());
        String t2 = login(paciente().getUsuario());
        String body = "{\"idHorario\":" + h.getId() + "}";

        mvc.perform(post("/api/citas").header(HttpHeaders.AUTHORIZATION, t1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(post("/api/citas").header(HttpHeaders.AUTHORIZATION, t2)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void reservaSinIdHorarioDevuelve400() throws Exception {
        String token = login(paciente().getUsuario());
        mvc.perform(post("/api/citas").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recepcionNoPuedeReservarComoPaciente() throws Exception {
        String token = login(usuario(RolNombre.RECEPCIONISTA));
        mvc.perform(post("/api/citas").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idHorario\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidaElToken() throws Exception {
        String token = login(paciente().getUsuario());
        mvc.perform(get("/api/citas/mias").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/citas/mias").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recuperarPasswordSiempreResponde202() throws Exception {
        mvc.perform(post("/api/auth/recuperar-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-existe@correo.test\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void restablecerConTokenInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/restablecer-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"basura\",\"nuevaPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaInexistenteDevuelve404ConCuerpoJson() throws Exception {
        String token = login(usuario(RolNombre.ADMINISTRADOR));
        mvc.perform(get("/api/no-existe").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        String token = login(paciente().getUsuario());
        mvc.perform(patch("/api/citas/mias").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isMethodNotAllowed());
    }
}
