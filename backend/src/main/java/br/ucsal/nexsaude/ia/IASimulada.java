package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * IA determinística por regras fixas. Padrão em desenvolvimento e plano B da apresentação:
 * não precisa de internet nem de chave.
 */
@Component
@ConditionalOnProperty(name = "nexsaude.ia.modo", havingValue = "simulada", matchIfMissing = true)
public class IASimulada implements AssistenteClinicoIA {

    private record Par(String a, String b, GravidadeInteracao gravidade, String descricao, String recomendacao) {}

    private static final List<Par> PARES = List.of(
            new Par("varfarina", "aas", GravidadeInteracao.GRAVE,
                    "Risco aumentado de sangramento (anticoagulante + antiagregante).",
                    "Evitar a associação ou monitorar INR e sinais de sangramento."),
            new Par("varfarina", "acido acetilsalicilico", GravidadeInteracao.GRAVE,
                    "Risco aumentado de sangramento (anticoagulante + antiagregante).",
                    "Evitar a associação ou monitorar INR e sinais de sangramento."),
            new Par("sildenafila", "nitroglicerina", GravidadeInteracao.GRAVE,
                    "Hipotensão grave por potencialização do efeito vasodilatador.",
                    "Associação contraindicada."),
            new Par("enalapril", "espironolactona", GravidadeInteracao.MODERADA,
                    "Risco de hipercalemia.",
                    "Monitorar potássio sérico e função renal."));

    @Override
    public ResultadoTriagemIA sugerirTriagem(DadosTriagemIA d) {
        SinaisVitais s = d.sinais();
        Integer spo2 = s == null ? null : s.spo2();
        Integer pas = s == null ? null : s.paSistolica();
        Integer fc = s == null ? null : s.fc();
        Double temp = s == null ? null : s.temperatura();

        if (menor(spo2, 90)) return r(CorRisco.VERMELHO, "Saturação de O₂ abaixo de 90%.");
        if (menor(pas, 90)) return r(CorRisco.VERMELHO, "Hipotensão (PA sistólica abaixo de 90 mmHg).");
        if (maior(fc, 140)) return r(CorRisco.VERMELHO, "Taquicardia acima de 140 bpm.");
        if (menor(spo2, 94)) return r(CorRisco.LARANJA, "Saturação de O₂ entre 90% e 93%.");
        if (temp != null && temp >= 39.5) return r(CorRisco.LARANJA, "Febre alta (≥ 39,5 °C).");
        if (maior(fc, 120)) return r(CorRisco.LARANJA, "Taquicardia acima de 120 bpm.");
        if (pas != null && pas >= 180) return r(CorRisco.LARANJA, "PA sistólica ≥ 180 mmHg.");
        if (temp != null && temp >= 38) return r(CorRisco.AMARELO, "Febre (≥ 38 °C).");
        if (maior(fc, 100)) return r(CorRisco.AMARELO, "Frequência cardíaca acima de 100 bpm.");
        if (d.sintomas() != null && !d.sintomas().isBlank())
            return r(CorRisco.VERDE, "Sinais vitais dentro da normalidade, com queixa ativa.");
        return r(CorRisco.AZUL, "Sem queixa relevante e sinais vitais normais.");
    }

    @Override
    public ResultadoInteracaoIA verificarInteracoes(List<String> prescritos, List<String> usoContinuo) {
        List<String> novos = prescritos.stream().map(IASimulada::normalizar).toList();
        List<String> todos = new ArrayList<>(novos);
        usoContinuo.stream().map(IASimulada::normalizar).forEach(todos::add);

        List<InteracaoIA> achadas = new ArrayList<>();
        for (Par p : PARES) {
            boolean temA = todos.stream().anyMatch(m -> m.contains(p.a()));
            boolean temB = todos.stream().anyMatch(m -> m.contains(p.b()));
            boolean envolveNovo = novos.stream().anyMatch(m -> m.contains(p.a()) || m.contains(p.b()));
            boolean jaAchada = achadas.stream().anyMatch(i -> i.descricao().equals(p.descricao()));
            if (temA && temB && envolveNovo && !jaAchada) {
                achadas.add(new InteracaoIA(List.of(p.a(), p.b()), p.gravidade(), p.descricao(), p.recomendacao()));
            }
        }
        return new ResultadoInteracaoIA(achadas);
    }

    @Override
    public String nomeImplementacao() { return "SIMULADA"; }

    @Override
    public String modelo() { return "regras-v1"; }

    private static ResultadoTriagemIA r(CorRisco cor, String justificativa) {
        return new ResultadoTriagemIA(cor, 0.7, justificativa);
    }

    private static boolean menor(Integer v, int limite) { return v != null && v < limite; }

    private static boolean maior(Integer v, int limite) { return v != null && v > limite; }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
