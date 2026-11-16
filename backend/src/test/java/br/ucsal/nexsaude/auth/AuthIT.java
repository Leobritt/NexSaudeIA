package br.ucsal.nexsaude.auth;

import br.ucsal.nexsaude.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegrationTest {

    private String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    @Test
    void loginValidoDevolveToken() throws Exception {
        usuario(Perfil.MEDICO);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("medico@teste.local", SENHA_TESTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.perfil").value("MEDICO"))
                .andExpect(jsonPath("$.expiraEm", notNullValue()));
    }

    @Test
    void adminInicialDaMigracaoConsegueLogar() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@nexsaude.local", "nexsaude123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void senhaErradaDevolve401() throws Exception {
        usuario(Perfil.MEDICO);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("medico@teste.local", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailInexistenteDevolve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ninguem@teste.local", "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semTokenDevolve401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        String expirado = new JwtService("dev-segredo-nexsaude-ia-trocar-em-producao-0123456789", -1)
                .gerar(usuario(Perfil.ADMIN));
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenLixoDevolve401() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesativadoDevolve401() throws Exception {
        Usuario u = usuarioRepository.save(new Usuario("Inativo", "inativo@teste.local",
                passwordEncoder.encode(SENHA_TESTE), Perfil.ADMIN));
        String token = jwtService.gerar(u);
        u.setAtivo(false);
        usuarioRepository.save(u);
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("inativo@teste.local", SENHA_TESTE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enfermeiroNaoListaUsuarios403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(Perfil.ENFERMEIRO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaSoParaAuditor() throws Exception {
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(Perfil.AUDITOR)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCriaUsuarioEEmailDuplicado422() throws Exception {
        String corpo = "{\"nome\":\"Bia\",\"email\":\"bia@teste.local\",\"senha\":\"segura123\",\"perfil\":\"ENFERMEIRO\"}";
        mvc.perform(post("/api/usuarios").header("Authorization", bearer(Perfil.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ENFERMEIRO"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(post("/api/usuarios").header("Authorization", bearer(Perfil.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void swaggerEPublico() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
