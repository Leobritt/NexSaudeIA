package br.ucsal.nexsaude;

import br.ucsal.nexsaude.auth.JwtService;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.auth.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base dos testes de integração: sobe um PostgreSQL 16 real, compartilhado entre as classes. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTest {

    public static final String SENHA_TESTE = "senha123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    /** Usuário de teste do perfil (criado na primeira vez), e-mail perfil@teste.local. */
    protected Usuario usuario(Perfil perfil) {
        String email = perfil.name().toLowerCase(Locale.ROOT) + "@teste.local";
        return usuarioRepository.findByEmail(email).orElseGet(() -> usuarioRepository.save(
                new Usuario("Teste " + perfil.name(), email, passwordEncoder.encode(SENHA_TESTE), perfil)));
    }

    /** Header Authorization pronto para o perfil. */
    protected String bearer(Perfil perfil) {
        return "Bearer " + jwtService.gerar(usuario(perfil));
    }

    // ---------- cenários clínicos via API ----------

    protected static final String SINAIS_NORMAIS =
            "{\"paSistolica\":120,\"paDiastolica\":80,\"temperatura\":36.8,\"spo2\":98,\"fc\":80,\"fr\":16}";

    protected long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected String postJson(String url, Perfil perfil, String corpo) throws Exception {
        return mvc.perform(post(url).header("Authorization", bearer(perfil))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getContentAsString();
    }

    /** Cadastra um paciente (com ou sem consentimento de IA) e devolve o id. */
    protected long novoPaciente(boolean consentimento, String... usoContinuo) throws Exception {
        String meds = usoContinuo.length == 0 ? "" : "\"" + String.join("\",\"", usoContinuo) + "\"";
        String corpo = """
                {"nome":"Paciente Teste","cpf":"%s","dataNascimento":"1970-01-01","sexo":"M",
                 "medicamentosUsoContinuo":[%s],"alergias":[]}""".formatted(CpfsTeste.novo(), meds);
        String resp = mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = id(resp);
        if (consentimento) {
            mvc.perform(put("/api/pacientes/{id}/consentimento-ia", id).header("Authorization", bearer(Perfil.ENFERMEIRO))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":true}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    protected long abrirAtendimento(long pacienteId) throws Exception {
        String resp = mvc.perform(post("/api/atendimentos").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pacienteId\":" + pacienteId + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(resp);
    }

    /** Triagem manual com a cor dada. */
    protected void triar(long atendimentoId, String cor) throws Exception {
        mvc.perform(post("/api/triagens").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"dor","sinais":%s,"cor":"%s"}"""
                                .formatted(atendimentoId, SINAIS_NORMAIS, cor)))
                .andExpect(status().isCreated());
    }

    protected void acaoMedica(long atendimentoId, String acao, String corpo) throws Exception {
        var req = post("/api/atendimentos/{id}/" + acao, atendimentoId).header("Authorization", bearer(Perfil.MEDICO));
        if (corpo != null) req = req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        mvc.perform(req).andExpect(status().isOk());
    }
}
