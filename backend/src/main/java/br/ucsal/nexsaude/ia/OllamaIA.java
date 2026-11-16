package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * IA gratuita e local via Ollama (modelo aberto rodando na própria máquina): os dados do paciente
 * não saem do computador. Ativa com nexsaude.ia.modo=ollama. Usa o endpoint /api/chat com
 * "format" = JSON Schema, para o modelo devolver sempre a estrutura esperada.
 */
@Component
@ConditionalOnProperty(name = "nexsaude.ia.modo", havingValue = "ollama")
public class OllamaIA implements AssistenteClinicoIA {

    private static final String INSTRUCAO_JSON = "\n\nResponda somente com o JSON pedido, sem texto fora dele.";

    private static final Map<String, Object> SCHEMA_TRIAGEM = Map.of(
            "type", "object",
            "properties", Map.of(
                    "cor", Map.of("type", "string", "enum", List.of("VERMELHO", "LARANJA", "AMARELO", "VERDE", "AZUL")),
                    "confianca", Map.of("type", "number"),
                    "justificativa", Map.of("type", "string")),
            "required", List.of("cor", "confianca", "justificativa"));

    private static final Map<String, Object> SCHEMA_INTERACAO = Map.of(
            "type", "object",
            "properties", Map.of("interacoes", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "medicamentos", Map.of("type", "array", "items", Map.of("type", "string")),
                                    "gravidade", Map.of("type", "string", "enum", List.of("LEVE", "MODERADA", "GRAVE")),
                                    "descricao", Map.of("type", "string"),
                                    "recomendacao", Map.of("type", "string")),
                            "required", List.of("medicamentos", "gravidade", "descricao", "recomendacao")))),
            "required", List.of("interacoes"));

    private final RestClient http;
    private final String modelo;
    private final JsonMapper json;
    private final String promptTriagem;
    private final String promptInteracao;

    @Autowired
    public OllamaIA(@Value("${nexsaude.ia.ollama.url}") String url,
                    @Value("${nexsaude.ia.ollama.modelo}") String modelo,
                    @Value("${nexsaude.ia.timeout-seg}") long timeoutSeg,
                    JsonMapper json) {
        this(url, modelo, json, RestClient.builder().requestFactory(fabrica(timeoutSeg)));
    }

    OllamaIA(String url, String modelo, JsonMapper json, RestClient.Builder builder) {
        this.http = builder.baseUrl(url).build();
        this.modelo = modelo;
        this.json = json;
        this.promptTriagem = ler("prompts/triagem-v1.txt") + INSTRUCAO_JSON;
        this.promptInteracao = ler("prompts/interacao-v1.txt") + INSTRUCAO_JSON;
    }

    @Override
    public ResultadoTriagemIA sugerirTriagem(DadosTriagemIA d) {
        var s = d.sinais();
        String pergunta = """
                Idade: %d anos. Sexo: %s.
                Sintomas relatados: %s
                Sinais vitais: PA %s/%s mmHg, temperatura %s °C, SpO2 %s%%, FC %s bpm, FR %s irpm.
                """.formatted(d.idade(), d.sexo(), d.sintomas(),
                s.paSistolica(), s.paDiastolica(), s.temperatura(), s.spo2(), s.fc(), s.fr());
        JsonNode r = chamar(promptTriagem, pergunta, SCHEMA_TRIAGEM);
        try {
            CorRisco cor = CorRisco.valueOf(r.get("cor").asString().trim().toUpperCase(Locale.ROOT));
            double confianca = Math.max(0, Math.min(1, r.get("confianca").asDouble()));
            return new ResultadoTriagemIA(cor, confianca, r.get("justificativa").asString());
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Resposta do Ollama com triagem inválida", e);
        }
    }

    @Override
    public ResultadoInteracaoIA verificarInteracoes(List<String> prescritos, List<String> usoContinuo) {
        String pergunta = "Medicamentos desta prescrição: " + String.join(", ", prescritos)
                + "\nMedicamentos de uso contínuo: " + (usoContinuo.isEmpty() ? "nenhum" : String.join(", ", usoContinuo));
        JsonNode r = chamar(promptInteracao, pergunta, SCHEMA_INTERACAO);
        try {
            List<InteracaoIA> lista = new ArrayList<>();
            for (JsonNode i : r.get("interacoes")) {
                List<String> meds = new ArrayList<>();
                i.get("medicamentos").forEach(m -> meds.add(m.asString()));
                lista.add(new InteracaoIA(meds,
                        GravidadeInteracao.valueOf(i.get("gravidade").asString().trim().toUpperCase(Locale.ROOT)),
                        i.get("descricao").asString(),
                        i.has("recomendacao") ? i.get("recomendacao").asString() : null));
            }
            return new ResultadoInteracaoIA(lista);
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Resposta do Ollama com interações inválidas", e);
        }
    }

    /** Chama /api/chat sem streaming e devolve o JSON que o modelo escreveu no conteúdo. */
    private JsonNode chamar(String system, String pergunta, Map<String, Object> schema) {
        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "stream", false,
                "format", schema,
                "options", Map.of("temperature", 0),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", pergunta)));
        String bruto;
        try {
            bruto = http.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(corpo)
                    .retrieve().body(String.class);
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Falha ao chamar o Ollama: " + e.getMessage(), e);
        }
        try {
            String conteudo = json.readTree(bruto).path("message").path("content").asString();
            return json.readTree(conteudo);
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Ollama devolveu algo que não é o JSON esperado", e);
        }
    }

    @Override
    public String nomeImplementacao() { return "OLLAMA"; }

    @Override
    public String modelo() { return modelo; }

    private static JdkClientHttpRequestFactory fabrica(long timeoutSeg) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(cliente);
        f.setReadTimeout(Duration.ofSeconds(timeoutSeg));
        return f;
    }

    private static String ler(String caminho) {
        try {
            return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
