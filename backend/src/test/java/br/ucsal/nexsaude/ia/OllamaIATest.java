package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaIATest {

    static final String URL = "http://ollama:11434";
    MockRestServiceServer servidor;
    OllamaIA ia;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        ia = new OllamaIA(URL, "qwen2.5:3b", JsonMapper.builder().build(), builder);
    }

    /** Resposta do /api/chat com o conteúdo (JSON em texto) que o modelo gerou. */
    static String resposta(String conteudo) {
        String escapado = conteudo.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"model\":\"qwen2.5:3b\",\"message\":{\"role\":\"assistant\",\"content\":\"" + escapado + "\"},\"done\":true}";
    }

    static DadosTriagemIA dados() {
        return new DadosTriagemIA(70, "F", "falta de ar", new SinaisVitais(120, 80, 36.8, 88, 90, 22));
    }

    @Test
    void triagemValidaChamaChatComSchemaESemStream() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen2.5:3b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format.properties.cor").exists())
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("SpO2 88")))
                .andRespond(withSuccess(resposta("{\"cor\":\"vermelho\",\"confianca\":0.9,\"justificativa\":\"Hipoxemia\"}"),
                        MediaType.APPLICATION_JSON));

        ResultadoTriagemIA r = ia.sugerirTriagem(dados());

        assertThat(r.cor()).isEqualTo(CorRisco.VERMELHO);
        assertThat(r.confianca()).isEqualTo(0.9);
        assertThat(r.justificativa()).isEqualTo("Hipoxemia");
        servidor.verify();
    }

    @Test
    void confiancaForaDoIntervaloEhLimitada() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andRespond(withSuccess(resposta("{\"cor\":\"VERDE\",\"confianca\":7,\"justificativa\":\"ok\"}"),
                        MediaType.APPLICATION_JSON));
        assertThat(ia.sugerirTriagem(dados()).confianca()).isEqualTo(1.0);
    }

    @Test
    void corInvalidaViraIndisponivel() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andRespond(withSuccess(resposta("{\"cor\":\"ROXO\",\"confianca\":0.5,\"justificativa\":\"?\"}"),
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> ia.sugerirTriagem(dados())).isInstanceOf(IAIndisponivelException.class);
    }

    @Test
    void conteudoQueNaoEhJsonViraIndisponivel() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andRespond(withSuccess(resposta("Acho que é amarelo"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> ia.sugerirTriagem(dados())).isInstanceOf(IAIndisponivelException.class);
    }

    @Test
    void erroHttpViraIndisponivel() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\":\"model not found\"}"));
        assertThatThrownBy(() -> ia.sugerirTriagem(dados()))
                .isInstanceOf(IAIndisponivelException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    void interacoesValidas() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andExpect(jsonPath("$.format.properties.interacoes").exists())
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("AAS")))
                .andRespond(withSuccess(resposta("""
                        {"interacoes":[{"medicamentos":["AAS","varfarina"],"gravidade":"grave",
                         "descricao":"Risco de sangramento","recomendacao":"Evitar"}]}"""), MediaType.APPLICATION_JSON));

        ResultadoInteracaoIA r = ia.verificarInteracoes(List.of("AAS"), List.of("varfarina"));

        assertThat(r.interacoes()).singleElement().satisfies(i -> {
            assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.GRAVE);
            assertThat(i.medicamentos()).containsExactly("AAS", "varfarina");
        });
    }

    @Test
    void gravidadeInvalidaViraIndisponivel() {
        servidor.expect(requestTo(URL + "/api/chat"))
                .andRespond(withSuccess(resposta("""
                        {"interacoes":[{"medicamentos":["a","b"],"gravidade":"PESSIMA","descricao":"x","recomendacao":"y"}]}"""),
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> ia.verificarInteracoes(List.of("a"), List.of("b")))
                .isInstanceOf(IAIndisponivelException.class);
    }

    @Test
    void identificaImplementacaoEModelo() {
        assertThat(ia.nomeImplementacao()).isEqualTo("OLLAMA");
        assertThat(ia.modelo()).isEqualTo("qwen2.5:3b");
    }
}
