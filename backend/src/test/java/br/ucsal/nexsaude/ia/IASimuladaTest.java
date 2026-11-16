package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IASimuladaTest {

    final IASimulada ia = new IASimulada();

    static SinaisVitais normais() {
        return new SinaisVitais(120, 80, 36.8, 98, 80, 16);
    }

    CorRisco cor(SinaisVitais s, String sintomas) {
        return ia.sugerirTriagem(new DadosTriagemIA(40, "F", sintomas, s)).cor();
    }

    @Test
    void vermelho() {
        assertThat(cor(new SinaisVitais(120, 80, 36.8, 89, 80, 16), "falta de ar")).isEqualTo(CorRisco.VERMELHO);
        assertThat(cor(new SinaisVitais(89, 50, 36.8, 98, 80, 16), "tontura")).isEqualTo(CorRisco.VERMELHO);
        assertThat(cor(new SinaisVitais(120, 80, 36.8, 98, 141, 16), "palpitação")).isEqualTo(CorRisco.VERMELHO);
    }

    @Test
    void laranja() {
        assertThat(cor(new SinaisVitais(120, 80, 36.8, 93, 80, 16), "tosse")).isEqualTo(CorRisco.LARANJA);
        assertThat(cor(new SinaisVitais(120, 80, 39.5, 98, 80, 16), "febre")).isEqualTo(CorRisco.LARANJA);
        assertThat(cor(new SinaisVitais(180, 110, 36.8, 98, 80, 16), "cefaleia")).isEqualTo(CorRisco.LARANJA);
    }

    @Test
    void amarelo() {
        assertThat(cor(new SinaisVitais(120, 80, 38.0, 98, 80, 16), "febre")).isEqualTo(CorRisco.AMARELO);
        assertThat(cor(new SinaisVitais(120, 80, 36.8, 98, 101, 16), "ansiedade")).isEqualTo(CorRisco.AMARELO);
    }

    @Test
    void limitesNaoDisparam() {
        assertThat(cor(new SinaisVitais(90, 60, 36.8, 94, 100, 16), "dor leve")).isEqualTo(CorRisco.VERDE);
    }

    @Test
    void verdeEAzul() {
        assertThat(cor(normais(), "dor de garganta")).isEqualTo(CorRisco.VERDE);
        assertThat(cor(normais(), "  ")).isEqualTo(CorRisco.AZUL);
    }

    @Test
    void sinaisAusentesContamComoNormais() {
        assertThat(cor(new SinaisVitais(null, null, null, null, null, null), "corte no dedo")).isEqualTo(CorRisco.VERDE);
    }

    @Test
    void justificativaEConfianca() {
        ResultadoTriagemIA r = ia.sugerirTriagem(new DadosTriagemIA(40, "F", "x", new SinaisVitais(120, 80, 36.8, 85, 80, 16)));
        assertThat(r.confianca()).isEqualTo(0.7);
        assertThat(r.justificativa()).containsIgnoringCase("satura");
    }

    @Test
    void interacaoGraveIgnoraCaixaEAcento() {
        ResultadoInteracaoIA r = ia.verificarInteracoes(List.of("AAS"), List.of("Varfarina"));
        assertThat(r.interacoes()).singleElement()
                .satisfies(i -> assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.GRAVE));
        assertThat(ia.verificarInteracoes(List.of("Ácido acetilsalicílico", "varfarina sódica"), List.of()).interacoes())
                .singleElement().satisfies(i -> assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.GRAVE));
    }

    @Test
    void outrasInteracoes() {
        assertThat(ia.verificarInteracoes(List.of("sildenafila"), List.of("nitroglicerina")).interacoes())
                .singleElement().satisfies(i -> assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.GRAVE));
        assertThat(ia.verificarInteracoes(List.of("enalapril", "espironolactona"), List.of()).interacoes())
                .singleElement().satisfies(i -> assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.MODERADA));
    }

    @Test
    void semInteracao() {
        assertThat(ia.verificarInteracoes(List.of("dipirona"), List.of("losartana")).interacoes()).isEmpty();
        // par só entre medicamentos já em uso contínuo não é novidade desta prescrição
        assertThat(ia.verificarInteracoes(List.of("dipirona"), List.of("varfarina", "aas")).interacoes()).isEmpty();
    }
}
