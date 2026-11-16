package br.ucsal.nexsaude;

import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.leito.Leito;
import br.ucsal.nexsaude.leito.Setor;
import br.ucsal.nexsaude.leito.StatusLeito;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-01 → UC-02 → UC-03 de ponta a ponta, como na apresentação. */
class FluxoCompletoIT extends IntegrationTest {

    @Test
    void triagemInternacaoEPrescricaoComAuditoria() throws Exception {
        // cadastro + consentimento (RN-002)
        String cpf = CpfsTeste.novo();
        long paciente = id(postJson("/api/pacientes", Perfil.ENFERMEIRO, """
                {"nome":"Dona Fluxo","cpf":"%s","dataNascimento":"1950-03-02","sexo":"F",
                 "medicamentosUsoContinuo":["Varfarina"],"alergias":[]}""".formatted(cpf)));
        mvc.perform(put("/api/pacientes/{id}/consentimento-ia", paciente).header("Authorization", bearer(Perfil.ENFERMEIRO))
                .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":true}")).andExpect(status().isOk());

        // UC-01: chegada, sugestão da IA, confirmação do enfermeiro
        long at = abrirAtendimento(paciente);
        String sug = postJson("/api/triagens/sugestao", Perfil.ENFERMEIRO, """
                {"atendimentoId":%d,"sintomas":"falta de ar e dor torácica",
                 "sinais":{"paSistolica":150,"paDiastolica":95,"temperatura":37.2,"spo2":92,"fc":110,"fr":24}}""".formatted(at));
        assertThat((String) JsonPath.read(sug, "$.cor")).isEqualTo("LARANJA");
        long sugestao = ((Number) JsonPath.read(sug, "$.sugestaoId")).longValue();
        String triagem = postJson("/api/triagens", Perfil.ENFERMEIRO, """
                {"atendimentoId":%d,"sintomas":"falta de ar e dor torácica",
                 "sinais":{"paSistolica":150,"paDiastolica":95,"temperatura":37.2,"spo2":92,"fc":110,"fr":24},
                 "cor":"LARANJA","sugestaoId":%d}""".formatted(at, sugestao));
        assertThat((String) JsonPath.read(triagem, "$.origem")).isEqualTo("IA_CONFIRMADA");

        // médico assume e indica internação
        Setor setor = setorRepository.save(new Setor("Fluxo " + System.nanoTime(), "ENFERMARIA"));
        Leito leito = leitoRepository.save(new Leito("FX" + System.nanoTime(), setor, StatusLeito.LIVRE));
        acaoMedica(at, "iniciar", null);
        acaoMedica(at, "indicar-internacao", "{\"setorId\":" + setor.getId() + "}");

        // UC-02: gestor aloca o leito
        String aguardando = mvc.perform(get("/api/atendimentos/aguardando-leito").header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(aguardando, "$[*].id")).contains((int) at);
        long internacao = id(postJson("/api/internacoes", Perfil.GESTOR_LEITOS,
                "{\"atendimentoId\":" + at + ",\"leitoId\":" + leito.getId() + "}"));

        // UC-03: prescrição com AAS → interação grave com varfarina → assina com justificativa
        long prescricao = id(postJson("/api/prescricoes", Perfil.MEDICO, "{\"internacaoId\":" + internacao
                + ",\"itens\":[{\"medicamento\":\"AAS\",\"dose\":\"100 mg\",\"via\":\"VO\",\"frequencia\":\"1x ao dia\"}]}"));
        mvc.perform(post("/api/prescricoes/{id}/verificar", prescricao).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(jsonPath("$.alertas[0].gravidade").value("GRAVE"));
        mvc.perform(post("/api/prescricoes/{id}/assinar", prescricao).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/prescricoes/{id}/assinar", prescricao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Síndrome coronariana; INR monitorado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSINADA"));

        // auditor encontra o rastro: chamadas à IA, triagem, internação e assinatura
        String auditoria = mvc.perform(get("/api/auditoria").param("size", "200").header("Authorization", bearer(Perfil.AUDITOR)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> acoes = JsonPath.read(auditoria, "$.content[*].acao");
        assertThat(acoes).contains("CHAMADA_IA", "DECISAO_IA", "TRIAR", "INTERNAR", "VERIFICAR", "ASSINAR");
        mvc.perform(get("/api/auditoria").param("recurso", "Prescricao").header("Authorization", bearer(Perfil.AUDITOR)))
                .andExpect(jsonPath("$.content[0].acao").value("ASSINAR"))
                .andExpect(jsonPath("$.content[0].usuario").value("medico@teste.local"));
    }
}
