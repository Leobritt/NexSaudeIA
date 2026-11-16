package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auth.Perfil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AtendimentoIT extends IntegrationTest {

    @Test
    void chegadaAbreAguardandoTriagem() throws Exception {
        long pac = novoPaciente(false);
        String resp = postJson("/api/atendimentos", Perfil.ENFERMEIRO, "{\"pacienteId\":" + pac + "}");
        assertThat((String) JsonPath.read(resp, "$.status")).isEqualTo("AGUARDANDO_TRIAGEM");
        assertThat((Integer) JsonPath.read(resp, "$.paciente.id")).isEqualTo((int) pac);
    }

    @Test
    void naoAbreSegundoAtendimentoAtivo() throws Exception {
        long pac = novoPaciente(false);
        abrirAtendimento(pac);
        mvc.perform(post("/api/atendimentos").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pacienteId\":" + pac + "}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void filaOrdenaPorCorDepoisChegada() throws Exception {
        long verde1 = abrirAtendimento(novoPaciente(false));
        long vermelho = abrirAtendimento(novoPaciente(false));
        long verde2 = abrirAtendimento(novoPaciente(false));
        long amarelo = abrirAtendimento(novoPaciente(false));
        triar(verde1, "VERDE");
        triar(vermelho, "VERMELHO");
        triar(verde2, "VERDE");
        triar(amarelo, "AMARELO");
        String fila = mvc.perform(get("/api/atendimentos/fila").header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(fila, "$[*].id");
        List<Integer> nossos = ids.stream().filter(i -> List.of((int) verde1, (int) vermelho, (int) verde2, (int) amarelo).contains(i)).toList();
        assertThat(nossos).containsExactly((int) vermelho, (int) amarelo, (int) verde1, (int) verde2);
        assertThat(ids.indexOf((int) vermelho)).isLessThan(ids.indexOf((int) amarelo));
    }

    @Test
    void naoTriadoNaoEntraNaFila() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        String fila = mvc.perform(get("/api/atendimentos/fila").header("Authorization", bearer(Perfil.MEDICO)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(fila, "$[*].id")).doesNotContain((int) at);
    }

    @Test
    void transicoesDoAtendimento() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "AMARELO");
        mvc.perform(post("/api/atendimentos/{id}/iniciar", at).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.medicoResponsavel").value("Teste MEDICO"));
        mvc.perform(post("/api/atendimentos/{id}/indicar-internacao", at).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"setorId\":" + setorDeTeste() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_LEITO"));
        String aguardando = mvc.perform(get("/api/atendimentos/aguardando-leito").header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(aguardando, "$[*].id")).contains((int) at);
    }

    @Test
    void finalizarDoProntoSocorro() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "VERDE");
        acaoMedica(at, "iniciar", null);
        mvc.perform(post("/api/atendimentos/{id}/finalizar", at).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    void transicaoInvalida422() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        mvc.perform(post("/api/atendimentos/{id}/finalizar", at).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/atendimentos/{id}/iniciar", at).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void atendimentoInexistente404() throws Exception {
        mvc.perform(post("/api/atendimentos/{id}/iniciar", 999999).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isNotFound());
    }

    @org.springframework.beans.factory.annotation.Autowired
    br.ucsal.nexsaude.leito.SetorRepository setores;

    long setorDeTeste() {
        return setores.save(new br.ucsal.nexsaude.leito.Setor("Setor " + System.nanoTime(), "ENFERMARIA")).getId();
    }
}
