package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auditoria.LogAuditoriaRepository;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.leito.InternacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProntuarioIT extends IntegrationTest {

    @Autowired InternacaoRepository internacoes;
    @Autowired LogAuditoriaRepository logs;

    long paciente;

    /** Paciente com atendimento, triagem, internação, prescrição e evolução. */
    @BeforeEach
    void cenario() throws Exception {
        long internacao = novaInternacao(true);
        paciente = internacoes.findById(internacao).orElseThrow().getPaciente().getId();
        postJson("/api/prescricoes", Perfil.MEDICO, "{\"internacaoId\":" + internacao
                + ",\"itens\":[{\"medicamento\":\"Dipirona\",\"dose\":\"1 g\",\"via\":\"EV\",\"frequencia\":\"6/6h\"}]}");
        mvc.perform(post("/api/pacientes/{id}/evolucoes", paciente).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Paciente estável, sem febre.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Teste MEDICO"));
    }

    @Test
    void medicoVeTudo() throws Exception {
        mvc.perform(get("/api/pacientes/{id}/prontuario", paciente).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.cpf").exists())
                .andExpect(jsonPath("$.atendimentos.length()").value(1))
                .andExpect(jsonPath("$.triagens.length()").value(1))
                .andExpect(jsonPath("$.internacoes.length()").value(1))
                .andExpect(jsonPath("$.prescricoes.length()").value(1))
                .andExpect(jsonPath("$.evolucoes[0].texto").value("Paciente estável, sem febre."));
    }

    @Test
    void auditorVeTudo() throws Exception {
        mvc.perform(get("/api/pacientes/{id}/prontuario", paciente).header("Authorization", bearer(Perfil.AUDITOR)))
                .andExpect(jsonPath("$.prescricoes.length()").value(1))
                .andExpect(jsonPath("$.evolucoes.length()").value(1));
    }

    @Test
    void enfermeiroNaoVePrescricoes() throws Exception {
        mvc.perform(get("/api/pacientes/{id}/prontuario", paciente).header("Authorization", bearer(Perfil.ENFERMEIRO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos.length()").value(1))
                .andExpect(jsonPath("$.triagens.length()").value(1))
                .andExpect(jsonPath("$.internacoes.length()").value(0))
                .andExpect(jsonPath("$.prescricoes.length()").value(0))
                .andExpect(jsonPath("$.evolucoes.length()").value(0));
    }

    @Test
    void gestorVeSoIdentificacaoEInternacoes() throws Exception {
        mvc.perform(get("/api/pacientes/{id}/prontuario", paciente).header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.nome").exists())
                .andExpect(jsonPath("$.paciente.cpf").exists())
                .andExpect(jsonPath("$.paciente.medicamentosUsoContinuo").isEmpty())
                .andExpect(jsonPath("$.paciente.alergias").isEmpty())
                .andExpect(jsonPath("$.internacoes.length()").value(1))
                .andExpect(jsonPath("$.triagens.length()").value(0))
                .andExpect(jsonPath("$.atendimentos.length()").value(0))
                .andExpect(jsonPath("$.prescricoes.length()").value(0));
    }

    @Test
    void leituraGeraLog() throws Exception {
        long antes = logs.count();
        mvc.perform(get("/api/pacientes/{id}/prontuario", paciente).header("Authorization", bearer(Perfil.AUDITOR)));
        assertThat(logs.findAll()).anySatisfy(l -> {
            assertThat(l.getAcao()).isEqualTo("LEITURA_PRONTUARIO");
            assertThat(l.getIdRecurso()).isEqualTo(String.valueOf(paciente));
            assertThat(l.getUsuario()).isEqualTo("auditor@teste.local");
        });
        assertThat(logs.count()).isEqualTo(antes + 1);
    }

    @Test
    void enfermeiroNaoEscreveEvolucao403() throws Exception {
        mvc.perform(post("/api/pacientes/{id}/evolucoes", paciente).header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void prontuarioDePacienteInexistente404() throws Exception {
        mvc.perform(get("/api/pacientes/{id}/prontuario", 999999).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isNotFound());
    }
}
