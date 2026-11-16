package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.CpfsTeste;
import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auditoria.LogAuditoriaRepository;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.paciente.ConsentimentoIA;
import br.ucsal.nexsaude.paciente.ConsentimentoIARepository;
import br.ucsal.nexsaude.paciente.Paciente;
import br.ucsal.nexsaude.paciente.PacienteRepository;
import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ServicoIAIT extends IntegrationTest {

    @MockitoBean AssistenteClinicoIA ia;
    @Autowired ServicoIA servico;
    @Autowired PacienteRepository pacientes;
    @Autowired ConsentimentoIARepository consentimentos;
    @Autowired SugestaoIARepository sugestoes;
    @Autowired LogAuditoriaRepository logs;

    static final SinaisVitais SINAIS = new SinaisVitais(120, 80, 36.8, 97, 88, 16);

    Paciente paciente(boolean consentimento) {
        Paciente p = pacientes.save(new Paciente("Fulano Secreto", CpfsTeste.novo(), null,
                LocalDate.now().minusYears(50), "M", List.of("varfarina"), List.of()));
        if (consentimento) consentimentos.save(new ConsentimentoIA(p.getId(), true, "teste"));
        return p;
    }

    void iaResponde() {
        when(ia.nomeImplementacao()).thenReturn("FAKE");
        when(ia.modelo()).thenReturn("fake-1");
        when(ia.sugerirTriagem(any())).thenReturn(new ResultadoTriagemIA(CorRisco.AMARELO, 0.8, "febre"));
    }

    @Test
    void semConsentimentoLanca422SemChamarIA() {
        iaResponde();
        Paciente p = paciente(false);
        assertThatThrownBy(() -> servico.sugerirTriagem(p, "febre", SINAIS, false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Paciente sem consentimento para uso de IA");
        verify(ia, never()).sugerirTriagem(any());
        assertThatThrownBy(() -> servico.verificarInteracoes(p, List.of("aas")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void semConsentimentoComRiscoIminentePermiteELogaMotivo() {
        iaResponde();
        Paciente p = paciente(false);
        SugestaoIA s = servico.sugerirTriagem(p, "dor no peito", SINAIS, true);
        assertThat(s.isSucesso()).isTrue();
        assertThat(s.getCor()).isEqualTo(CorRisco.AMARELO);
        assertThat(logs.findAll()).anySatisfy(l -> {
            assertThat(l.getRecurso()).isEqualTo("SugestaoIA");
            assertThat(l.getIdRecurso()).isEqualTo(s.getId().toString());
            assertThat(l.getDetalhes()).containsEntry("baseLegal", "RISCO_IMINENTE_SEM_CONSENTIMENTO");
        });
    }

    @Test
    void entradaNaoContemNomeNemCpfESugestaoFicaPendente() {
        iaResponde();
        Paciente p = paciente(true);
        SugestaoIA s = servico.sugerirTriagem(p, "febre", SINAIS, false);
        SugestaoIA salva = sugestoes.findById(s.getId()).orElseThrow();
        assertThat(salva.getStatus()).isEqualTo(StatusSugestao.PENDENTE);
        assertThat(salva.getEntrada().toString()).doesNotContain("Fulano").doesNotContain(p.getCpf());
        assertThat(salva.getEntrada()).containsEntry("idade", 50).containsEntry("sexo", "M");
        assertThat(salva.getVersaoPrompt()).isEqualTo("triagem-v1");
        assertThat(salva.getModelo()).isEqualTo("fake-1");
        assertThat(salva.getImplementacao()).isEqualTo("FAKE");
        assertThat(salva.getRespostaBruta()).contains("AMARELO");
    }

    @Test
    void falhaDaIAGravaSugestaoIndisponivelSemLancar() {
        when(ia.nomeImplementacao()).thenReturn("FAKE");
        when(ia.modelo()).thenReturn("fake-1");
        when(ia.sugerirTriagem(any())).thenThrow(new IAIndisponivelException("timeout"));
        SugestaoIA s = servico.sugerirTriagem(paciente(true), "febre", SINAIS, false);
        assertThat(s.isSucesso()).isFalse();
        assertThat(s.getCor()).isNull();
        assertThat(s.getMotivoFalha()).contains("timeout");
    }

    @Test
    void erroInesperadoDaIATambemViraIndisponivel() {
        when(ia.nomeImplementacao()).thenReturn("FAKE");
        when(ia.modelo()).thenReturn("fake-1");
        when(ia.verificarInteracoes(anyList(), anyList())).thenThrow(new IllegalStateException("json quebrado"));
        SugestaoIA s = servico.verificarInteracoes(paciente(true), List.of("aas"));
        assertThat(s.isSucesso()).isFalse();
    }

    @Test
    void verificarInteracoesUsaMedicamentosEmUsoContinuo() {
        when(ia.nomeImplementacao()).thenReturn("FAKE");
        when(ia.modelo()).thenReturn("fake-1");
        when(ia.verificarInteracoes(List.of("aas"), List.of("varfarina"))).thenReturn(new ResultadoInteracaoIA(List.of(
                new InteracaoIA(List.of("aas", "varfarina"), GravidadeInteracao.GRAVE, "sangramento", "evitar"))));
        SugestaoIA s = servico.verificarInteracoes(paciente(true), List.of("aas"));
        assertThat(s.isSucesso()).isTrue();
        assertThat(s.getInteracoes()).singleElement()
                .satisfies(i -> assertThat(i.gravidade()).isEqualTo(GravidadeInteracao.GRAVE));
        assertThat(s.getVersaoPrompt()).isEqualTo("interacao-v1");
    }

    @Test
    void cadaChamadaGeraLog() {
        iaResponde();
        long antes = logs.count();
        servico.sugerirTriagem(paciente(true), "febre", SINAIS, false);
        assertThat(logs.count()).isEqualTo(antes + 1);
    }

    @Test
    void marcarDecisao() {
        iaResponde();
        SugestaoIA s = servico.sugerirTriagem(paciente(true), "febre", SINAIS, false);
        servico.marcarDecisao(s.getId(), StatusSugestao.CONFIRMADA);
        assertThat(sugestoes.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(StatusSugestao.CONFIRMADA);
    }
}
