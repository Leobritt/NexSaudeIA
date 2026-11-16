package br.ucsal.nexsaude.revisao;

import br.ucsal.nexsaude.CpfsTeste;
import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auditoria.LogAuditoriaRepository;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.ia.IASimulada;
import br.ucsal.nexsaude.ia.InteracaoIA;
import br.ucsal.nexsaude.ia.ResultadoInteracaoIA;
import br.ucsal.nexsaude.ia.ResultadoTriagemIA;
import br.ucsal.nexsaude.leito.Leito;
import br.ucsal.nexsaude.leito.Setor;
import br.ucsal.nexsaude.leito.StatusLeito;
import br.ucsal.nexsaude.prescricao.GravidadeInteracao;
import br.ucsal.nexsaude.prescricao.Prescricao;
import br.ucsal.nexsaude.prescricao.PrescricaoRepository;
import br.ucsal.nexsaude.triagem.Atendimento;
import br.ucsal.nexsaude.triagem.AtendimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes que reproduzem os achados da revisão final do branch. */
class AchadosRevisaoIT extends IntegrationTest {

    @MockitoSpyBean IASimulada ia;
    @Autowired LogAuditoriaRepository logs;
    @Autowired AtendimentoRepository atendimentos;
    @Autowired PrescricaoRepository prescricoes;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void limpaSpy() { reset(ia); }

    // C1: gestor não pode "higienizar" leito ocupado pelo PATCH
    @Test
    void c1_patchNaoMexeEmLeitoOcupado() throws Exception {
        long internacao = novaInternacao(false);
        String leito = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/leitos").header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andReturn().getResponse().getContentAsString();
        Integer leitoId = com.jayway.jsonpath.JsonPath.<List<Integer>>read(leito,
                "$[?(@.pacienteAtual != null && @.status == 'OCUPADO')].id").getLast();
        mvc.perform(patch("/api/leitos/{id}/status", leitoId).header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIGIENIZACAO\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/internacoes/{id}/alta", internacao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    // I1: atualização com versão velha do atendimento é rejeitada
    @Test
    void i1_atendimentoComVersaoVelhaFalha() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        Atendimento velho = atendimentos.findById(at).orElseThrow();
        triar(at, "VERDE");
        assertThatThrownBy(() -> atendimentos.save(velho)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    // I1: prescrição com versão velha é rejeitada
    @Test
    void i1_prescricaoComVersaoVelhaFalha() throws Exception {
        long internacao = novaInternacao(true);
        long p = id(postJson("/api/prescricoes", Perfil.MEDICO, "{\"internacaoId\":" + internacao
                + ",\"itens\":[{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"EV\",\"frequencia\":\"6/6h\"}]}"));
        Prescricao velha = tx.execute(s -> {
            Prescricao x = prescricoes.findById(p).orElseThrow();
            x.getItens();
            x.getAlertas();
            return x;
        });
        mvc.perform(post("/api/prescricoes/{id}/verificar", p).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk());
        assertThatThrownBy(() -> prescricoes.save(velha)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    // I2: operação desfeita não deixa registro na auditoria
    @Test
    void i2_internacaoRecusadaNaoDeixaLogFalso() throws Exception {
        Setor s = setorRepository.save(new Setor("Rev " + System.nanoTime(), "ENFERMARIA"));
        Leito interditado = leitoRepository.save(new Leito("RV" + System.nanoTime(), s, StatusLeito.INTERDITADO));
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "AMARELO");
        acaoMedica(at, "iniciar", null);
        acaoMedica(at, "indicar-internacao", "{\"setorId\":" + s.getId() + "}");
        mvc.perform(post("/api/internacoes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atendimentoId\":" + at + ",\"leitoId\":" + interditado.getId() + "}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(logs.findAll()).noneSatisfy(l -> {
            assertThat(l.getRecurso()).isEqualTo("Atendimento");
            assertThat(l.getIdRecurso()).isEqualTo(String.valueOf(at));
            assertThat(l.getDetalhes()).containsEntry("para", "INTERNADO");
        });
    }

    // I3: valores fora do domínio do banco viram 400, não 409
    @Test
    void i3_sexoInvalidoENomeLongoViram400() throws Exception {
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"X","cpf":"%s","dataNascimento":"1980-01-01","sexo":"Feminino"}"""
                                .formatted(CpfsTeste.novo())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sexo").exists());
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"%s","cpf":"%s","dataNascimento":"1980-01-01","sexo":"F"}"""
                                .formatted("A".repeat(151), CpfsTeste.novo())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists());
        long internacao = novaInternacao(false);
        mvc.perform(post("/api/prescricoes").header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"internacaoId\":" + internacao
                                + ",\"itens\":[{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"" + "V".repeat(31)
                                + "\",\"frequencia\":\"6/6h\"}]}"))
                .andExpect(status().isBadRequest());
    }

    // I4: a chamada à IA não segura transação (nem conexão) do banco
    @Test
    void i4_iaChamadaForaDeTransacao() throws Exception {
        AtomicBoolean emTransacao = new AtomicBoolean(true);
        doAnswer(inv -> {
            emTransacao.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new ResultadoTriagemIA(br.ucsal.nexsaude.triagem.CorRisco.VERDE, 0.7, "ok");
        }).when(ia).sugerirTriagem(any());
        long at = abrirAtendimento(novoPaciente(true));
        mvc.perform(post("/api/triagens/sugestao").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"dor","sinais":%s}""".formatted(at, SINAIS_NORMAIS)))
                .andExpect(status().isOk());
        assertThat(emTransacao).isFalse();

        AtomicBoolean emTransacaoInteracao = new AtomicBoolean(true);
        doAnswer(inv -> {
            emTransacaoInteracao.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new ResultadoInteracaoIA(List.of());
        }).when(ia).verificarInteracoes(anyList(), anyList());
        long internacao = novaInternacao(true);
        long p = id(postJson("/api/prescricoes", Perfil.MEDICO, "{\"internacaoId\":" + internacao
                + ",\"itens\":[{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"EV\",\"frequencia\":\"6/6h\"}]}"));
        mvc.perform(post("/api/prescricoes/{id}/verificar", p).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVerificacao").value("VERIFICADA_IA"));
        assertThat(emTransacaoInteracao).isFalse();
    }

    // M1 (reclassificado): interação com campos nulos vinda da IA não pode dar 500
    @Test
    void m1_interacaoComCamposNulosViraIaIndisponivel() throws Exception {
        doReturn(new ResultadoInteracaoIA(Arrays.asList(
                new InteracaoIA(null, GravidadeInteracao.GRAVE, null, null))))
                .when(ia).verificarInteracoes(anyList(), anyList());
        long internacao = novaInternacao(true);
        long p = id(postJson("/api/prescricoes", Perfil.MEDICO, "{\"internacaoId\":" + internacao
                + ",\"itens\":[{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"EV\",\"frequencia\":\"6/6h\"}]}"));
        mvc.perform(post("/api/prescricoes/{id}/verificar", p).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVerificacao").value("IA_INDISPONIVEL"));
    }
}
