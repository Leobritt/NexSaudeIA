package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** IA real via Claude API, com saída estruturada (JSON validado pelo SDK). Ativa com nexsaude.ia.modo=claude. */
@Component
@ConditionalOnProperty(name = "nexsaude.ia.modo", havingValue = "claude")
public class ClaudeIA implements AssistenteClinicoIA {

    record RespostaTriagem(
            @JsonPropertyDescription("VERMELHO, LARANJA, AMARELO, VERDE ou AZUL") String cor,
            @JsonPropertyDescription("Confiança entre 0 e 1") double confianca,
            @JsonPropertyDescription("Justificativa clínica curta em português") String justificativa) {}

    record InteracaoClaude(
            List<String> medicamentos,
            @JsonPropertyDescription("LEVE, MODERADA ou GRAVE") String gravidade,
            String descricao,
            String recomendacao) {}

    record RespostaInteracao(List<InteracaoClaude> interacoes) {}

    private final AnthropicClient client;
    private final String modelo;
    private final String promptTriagem;
    private final String promptInteracao;

    public ClaudeIA(@Value("${nexsaude.ia.modelo}") String modelo,
                    @Value("${nexsaude.ia.timeout-seg}") long timeoutSeg) throws IOException {
        this.client = AnthropicOkHttpClient.builder().fromEnv()
                .timeout(Duration.ofSeconds(timeoutSeg))
                .maxRetries(0)
                .build();
        this.modelo = modelo;
        this.promptTriagem = ler("prompts/triagem-v1.txt");
        this.promptInteracao = ler("prompts/interacao-v1.txt");
    }

    @Override
    public ResultadoTriagemIA sugerirTriagem(DadosTriagemIA d) {
        String pergunta = """
                Idade: %d anos. Sexo: %s.
                Sintomas relatados: %s
                Sinais vitais: PA %s/%s mmHg, temperatura %s °C, SpO2 %s%%, FC %s bpm, FR %s irpm.
                """.formatted(d.idade(), d.sexo(), d.sintomas(),
                d.sinais().paSistolica(), d.sinais().paDiastolica(), d.sinais().temperatura(),
                d.sinais().spo2(), d.sinais().fc(), d.sinais().fr());
        RespostaTriagem r = chamar(promptTriagem, pergunta, RespostaTriagem.class);
        try {
            CorRisco cor = CorRisco.valueOf(r.cor().trim().toUpperCase(Locale.ROOT));
            double confianca = Math.max(0, Math.min(1, r.confianca()));
            return new ResultadoTriagemIA(cor, confianca, r.justificativa());
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Resposta da IA com cor inválida: " + r.cor(), e);
        }
    }

    @Override
    public ResultadoInteracaoIA verificarInteracoes(List<String> prescritos, List<String> usoContinuo) {
        String pergunta = "Medicamentos desta prescrição: " + String.join(", ", prescritos)
                + "\nMedicamentos de uso contínuo: " + (usoContinuo.isEmpty() ? "nenhum" : String.join(", ", usoContinuo));
        RespostaInteracao r = chamar(promptInteracao, pergunta, RespostaInteracao.class);
        try {
            return new ResultadoInteracaoIA(r.interacoes().stream()
                    .map(i -> new InteracaoIA(i.medicamentos(),
                            GravidadeInteracao.valueOf(i.gravidade().trim().toUpperCase(Locale.ROOT)),
                            i.descricao(), i.recomendacao()))
                    .toList());
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Resposta da IA com gravidade inválida", e);
        }
    }

    private <T> T chamar(String system, String pergunta, Class<T> tipo) {
        try {
            StructuredMessage<T> msg = client.messages().create(MessageCreateParams.builder()
                    .model(modelo)
                    .maxTokens(4000L)
                    .system(system)
                    .outputConfig(tipo)
                    .addUserMessage(pergunta)
                    .build());
            StopReason motivo = msg.stopReason().orElse(null);
            if (StopReason.REFUSAL.equals(motivo) || StopReason.MAX_TOKENS.equals(motivo)) {
                throw new IAIndisponivelException("IA encerrou sem resposta utilizável: " + motivo);
            }
            return msg.content().stream()
                    .flatMap(b -> b.text().stream())
                    .map(t -> t.text())
                    .findFirst()
                    .orElseThrow(() -> new IAIndisponivelException("IA não devolveu conteúdo"));
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IAIndisponivelException("Falha ao chamar a IA: " + e.getMessage(), e);
        }
    }

    @Override
    public String nomeImplementacao() { return "CLAUDE"; }

    @Override
    public String modelo() { return modelo; }

    private static String ler(String caminho) throws IOException {
        return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
    }
}
