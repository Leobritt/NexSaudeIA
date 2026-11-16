package br.ucsal.nexsaude.prescricao;

import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auditoria.LogAuditoriaRepository;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.ia.IAIndisponivelException;
import br.ucsal.nexsaude.ia.IASimulada;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrescricaoIT extends IntegrationTest {

    @MockitoSpyBean IASimulada ia;
    @Autowired LogAuditoriaRepository logs;

    static final String AAS = "{\"medicamento\":\"AAS\",\"dose\":\"100 mg\",\"via\":\"VO\",\"frequencia\":\"1x ao dia\"}";
    static final String DIPIRONA = "{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"EV\",\"frequencia\":\"6/6h\"}";

    @BeforeEach
    void limpaSpy() { reset(ia); }

    long prescrever(long internacao, String... itens) throws Exception {
        String resp = mvc.perform(post("/api/prescricoes").header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"internacaoId\":" + internacao + ",\"itens\":[" + String.join(",", itens) + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.statusVerificacao").value("NAO_VERIFICADA"))
                .andReturn().getResponse().getContentAsString();
        return id(resp);
    }

    ResultActions verificar(long id) throws Exception {
        return mvc.perform(post("/api/prescricoes/{id}/verificar", id).header("Authorization", bearer(Perfil.MEDICO)));
    }

    ResultActions assinar(long id, String justificativa) throws Exception {
        var req = post("/api/prescricoes/{id}/assinar", id).header("Authorization", bearer(Perfil.MEDICO));
        if (justificativa != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"" + justificativa + "\"}");
        }
        return mvc.perform(req);
    }

    @Test
    void assinarSemVerificar422() throws Exception {
        long p = prescrever(novaInternacao(true), DIPIRONA);
        assinar(p, null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void semInteracaoAssinaDireto() throws Exception {
        long p = prescrever(novaInternacao(true), DIPIRONA);
        verificar(p).andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVerificacao").value("VERIFICADA_IA"))
                .andExpect(jsonPath("$.alertas.length()").value(0));
        String resp = assinar(p, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSINADA"))
                .andExpect(jsonPath("$.assinadaEm").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(resp, "$.hashAssinatura")).matches("[0-9a-f]{64}");
    }

    @Test
    void interacaoGraveSemJustificativa422() throws Exception {
        long p = prescrever(novaInternacao(true, "varfarina"), AAS);
        verificar(p).andExpect(jsonPath("$.alertas[0].gravidade").value("GRAVE"));
        assinar(p, null).andExpect(status().isUnprocessableContent());
        assinar(p, "   ").andExpect(status().isUnprocessableContent());
    }

    @Test
    void interacaoGraveComJustificativaAssinaELoga() throws Exception {
        long p = prescrever(novaInternacao(true, "varfarina"), AAS);
        verificar(p);
        assinar(p, "Benefício supera o risco, INR monitorado")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justificativa").value("Benefício supera o risco, INR monitorado"));
        assertThat(logs.findAll()).anySatisfy(l -> {
            assertThat(l.getAcao()).isEqualTo("ASSINAR");
            assertThat(l.getIdRecurso()).isEqualTo(String.valueOf(p));
            assertThat(l.getDetalhes()).containsEntry("interacaoGrave", true);
        });
    }

    @Test
    void editarDepoisDeVerificarExigeNovaVerificacao() throws Exception {
        long p = prescrever(novaInternacao(true, "varfarina"), AAS);
        verificar(p).andExpect(jsonPath("$.alertas.length()").value(1));
        mvc.perform(put("/api/prescricoes/{id}/itens", p).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[" + DIPIRONA + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVerificacao").value("NAO_VERIFICADA"))
                .andExpect(jsonPath("$.alertas.length()").value(0))
                .andExpect(jsonPath("$.itens[0].medicamento").value("Dipirona"));
        assinar(p, null).andExpect(status().isUnprocessableContent());
        verificar(p);
        assinar(p, null).andExpect(status().isOk());
    }

    @Test
    void iaIndisponivelExigeJustificativa() throws Exception {
        doThrow(new IAIndisponivelException("timeout")).when(ia).verificarInteracoes(anyList(), anyList());
        long p = prescrever(novaInternacao(true), DIPIRONA);
        verificar(p).andExpect(jsonPath("$.statusVerificacao").value("IA_INDISPONIVEL"));
        assinar(p, null).andExpect(status().isUnprocessableContent());
        assinar(p, "IA fora do ar, conferido manualmente").andExpect(status().isOk());
        assertThat(logs.findAll()).anySatisfy(l -> {
            assertThat(l.getAcao()).isEqualTo("ASSINAR");
            assertThat(l.getIdRecurso()).isEqualTo(String.valueOf(p));
            assertThat(l.getDetalhes()).containsEntry("semVerificacaoIa", true);
        });
    }

    @Test
    void semConsentimentoViraIaIndisponivel() throws Exception {
        long p = prescrever(novaInternacao(false), DIPIRONA);
        verificar(p).andExpect(status().isOk()).andExpect(jsonPath("$.statusVerificacao").value("IA_INDISPONIVEL"));
    }

    @Test
    void hashMudaSeItensMudam() throws Exception {
        long internacao = novaInternacao(true);
        long p1 = prescrever(internacao, DIPIRONA);
        long p2 = prescrever(internacao, DIPIRONA.replace("1 g", "500 mg"));
        verificar(p1);
        verificar(p2);
        String h1 = JsonPath.read(assinar(p1, null).andReturn().getResponse().getContentAsString(), "$.hashAssinatura");
        String h2 = JsonPath.read(assinar(p2, null).andReturn().getResponse().getContentAsString(), "$.hashAssinatura");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void assinadaNaoEdita422() throws Exception {
        long p = prescrever(novaInternacao(true), DIPIRONA);
        verificar(p);
        assinar(p, null);
        mvc.perform(put("/api/prescricoes/{id}/itens", p).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[" + AAS + "]}"))
                .andExpect(status().isUnprocessableContent());
        verificar(p).andExpect(status().isUnprocessableContent());
        assinar(p, null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void internacaoEncerradaNaoRecebePrescricao() throws Exception {
        long internacao = novaInternacao(true);
        mvc.perform(post("/api/internacoes/{id}/alta", internacao).header("Authorization", bearer(Perfil.MEDICO))
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"alta\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/prescricoes").header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"internacaoId\":" + internacao + ",\"itens\":[" + DIPIRONA + "]}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void enfermeiroNaoPrescreve403() throws Exception {
        long internacao = novaInternacao(true);
        mvc.perform(post("/api/prescricoes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"internacaoId\":" + internacao + ",\"itens\":[" + DIPIRONA + "]}"))
                .andExpect(status().isForbidden());
    }
}
