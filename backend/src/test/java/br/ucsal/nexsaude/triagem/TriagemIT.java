package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.ia.AssistenteClinicoIA;
import br.ucsal.nexsaude.ia.IAIndisponivelException;
import br.ucsal.nexsaude.ia.IASimulada;
import br.ucsal.nexsaude.ia.StatusSugestao;
import br.ucsal.nexsaude.ia.SugestaoIARepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TriagemIT extends IntegrationTest {

    @MockitoSpyBean IASimulada ia;
    @Autowired TriagemRepository triagens;
    @Autowired SugestaoIARepository sugestoes;

    static final String SPO2_BAIXA =
            "{\"paSistolica\":120,\"paDiastolica\":80,\"temperatura\":36.8,\"spo2\":88,\"fc\":80,\"fr\":16}";

    @BeforeEach
    void limpaSpy() { reset(ia); }

    String sugerir(long atendimentoId, String sinais) throws Exception {
        return mvc.perform(post("/api/triagens/sugestao").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"falta de ar","sinais":%s}""".formatted(atendimentoId, sinais)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    String gravar(long atendimentoId, String cor, Long sugestaoId) throws Exception {
        return mvc.perform(post("/api/triagens").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"falta de ar","sinais":%s,"cor":"%s","sugestaoId":%s}"""
                                .formatted(atendimentoId, SPO2_BAIXA, cor, sugestaoId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    long sugestaoId(String json) {
        return ((Number) JsonPath.read(json, "$.sugestaoId")).longValue();
    }

    @Test
    void sugestaoNaoGravaTriagem() throws Exception {
        long at = abrirAtendimento(novoPaciente(true));
        long antes = triagens.count();
        String s = sugerir(at, SPO2_BAIXA);
        assertThat((String) JsonPath.read(s, "$.cor")).isEqualTo("VERMELHO");
        assertThat((Boolean) JsonPath.read(s, "$.indisponivel")).isFalse();
        assertThat((String) JsonPath.read(s, "$.implementacao")).isEqualTo("SIMULADA");
        assertThat(triagens.count()).isEqualTo(antes);
    }

    @Test
    void confirmarSugestaoGravaOrigemIaConfirmada() throws Exception {
        long at = abrirAtendimento(novoPaciente(true));
        long sug = sugestaoId(sugerir(at, SPO2_BAIXA));
        String t = gravar(at, "VERMELHO", sug);
        assertThat((String) JsonPath.read(t, "$.origem")).isEqualTo("IA_CONFIRMADA");
        assertThat((String) JsonPath.read(t, "$.enfermeiro")).isEqualTo("Teste ENFERMEIRO");
        assertThat(sugestoes.findById(sug).orElseThrow().getStatus()).isEqualTo(StatusSugestao.CONFIRMADA);
    }

    @Test
    void corDiferenteGravaReclassificada() throws Exception {
        long at = abrirAtendimento(novoPaciente(true));
        long sug = sugestaoId(sugerir(at, SPO2_BAIXA));
        String t = gravar(at, "LARANJA", sug);
        assertThat((String) JsonPath.read(t, "$.origem")).isEqualTo("IA_RECLASSIFICADA");
        assertThat(sugestoes.findById(sug).orElseThrow().getStatus()).isEqualTo(StatusSugestao.AJUSTADA);
    }

    @Test
    void sugestaoNaoPodeSerReusada() throws Exception {
        long at = abrirAtendimento(novoPaciente(true));
        long sug = sugestaoId(sugerir(at, SPO2_BAIXA));
        gravar(at, "VERMELHO", sug);
        mvc.perform(post("/api/triagens").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"x","sinais":%s,"cor":"VERDE","sugestaoId":%d}"""
                                .formatted(at, SPO2_BAIXA, sug)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void sugestaoDeOutroPacienteRecusada() throws Exception {
        long at1 = abrirAtendimento(novoPaciente(true));
        long at2 = abrirAtendimento(novoPaciente(true));
        long sug = sugestaoId(sugerir(at1, SPO2_BAIXA));
        mvc.perform(post("/api/triagens").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"x","sinais":%s,"cor":"VERDE","sugestaoId":%d}"""
                                .formatted(at2, SPO2_BAIXA, sug)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void semConsentimentoSugestao422() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        mvc.perform(post("/api/triagens/sugestao").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"x","sinais":%s}""".formatted(at, SPO2_BAIXA)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Paciente sem consentimento para uso de IA"));
    }

    @Test
    void iaIndisponivelPermiteTriagemManual() throws Exception {
        doThrow(new IAIndisponivelException("timeout")).when(ia).sugerirTriagem(any());
        long at = abrirAtendimento(novoPaciente(true));
        String s = sugerir(at, SPO2_BAIXA);
        assertThat((Boolean) JsonPath.read(s, "$.indisponivel")).isTrue();
        assertThat((Object) JsonPath.read(s, "$.cor")).isNull();
        String t = gravar(at, "LARANJA", sugestaoId(s));
        assertThat((String) JsonPath.read(t, "$.origem")).isEqualTo("MANUAL");
        String semSugestao = gravar(abrirAtendimento(novoPaciente(false)), "VERDE", null);
        assertThat((String) JsonPath.read(semSugestao, "$.origem")).isEqualTo("MANUAL");
    }

    @Test
    void reclassificacaoCriaNovaLinhaEPreservaAnterior() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "VERDE");
        triar(at, "LARANJA");
        List<Triagem> doAtendimento = triagens.findByAtendimentoIdOrderByDataHoraAscIdAsc(at);
        assertThat(doAtendimento).extracting(Triagem::getCor).containsExactly(CorRisco.VERDE, CorRisco.LARANJA);
        mvc.perform(get("/api/atendimentos/fila").header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(jsonPath("$[?(@.id == %d)].corAtual".formatted(at)).value("LARANJA"));
    }

    @Test
    void enfermeiroSoPodeTriarAtendimentoAberto() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "VERDE");
        acaoMedica(at, "iniciar", null);
        acaoMedica(at, "finalizar", null);
        mvc.perform(post("/api/triagens").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"x","sinais":%s,"cor":"VERDE"}""".formatted(at, SINAIS_NORMAIS)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void medicoNaoTria403() throws Exception {
        long at = abrirAtendimento(novoPaciente(true));
        mvc.perform(post("/api/triagens/sugestao").header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"atendimentoId":%d,"sintomas":"x","sinais":%s}""".formatted(at, SPO2_BAIXA)))
                .andExpect(status().isForbidden());
    }
}
