package br.ucsal.nexsaude;

import br.ucsal.nexsaude.auth.JwtService;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.auth.UsuarioRepository;
import br.ucsal.nexsaude.leito.Leito;
import br.ucsal.nexsaude.leito.LeitoRepository;
import br.ucsal.nexsaude.leito.Setor;
import br.ucsal.nexsaude.leito.SetorRepository;
import br.ucsal.nexsaude.leito.StatusLeito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base dos testes de integração: um PostgreSQL 16 real, compartilhado por todas as classes. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    public static final String SENHA_TESTE = "senha123";

    /** Um único container para toda a execução (o Ryuk remove ao fim da JVM). */
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;
    @Autowired protected SetorRepository setorRepository;
    @Autowired protected LeitoRepository leitoRepository;

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

    /** Paciente internado num leito novo; devolve o id da internação. */
    protected long novaInternacao(boolean consentimento, String... usoContinuo) throws Exception {
        long sufixo = System.nanoTime();
        Setor setor = setorRepository.save(new Setor("Setor " + sufixo, "ENFERMARIA"));
        Leito leito = leitoRepository.save(new Leito("L" + sufixo, setor, StatusLeito.LIVRE));
        long at = abrirAtendimento(novoPaciente(consentimento, usoContinuo));
        triar(at, "AMARELO");
        acaoMedica(at, "iniciar", null);
        acaoMedica(at, "indicar-internacao", "{\"setorId\":" + setor.getId() + "}");
        String resp = mvc.perform(post("/api/internacoes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atendimentoId\":" + at + ",\"leitoId\":" + leito.getId() + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(resp);
    }
}
