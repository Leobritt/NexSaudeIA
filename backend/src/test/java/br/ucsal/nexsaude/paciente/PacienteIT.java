package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.CpfsTeste;
import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auditoria.LogAuditoriaRepository;
import br.ucsal.nexsaude.auth.Perfil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteIT extends IntegrationTest {

    @Autowired LogAuditoriaRepository logs;

    static String json(String nome, String cpf) {
        return """
                {"nome":"%s","cpf":"%s","dataNascimento":"1980-05-17","sexo":"F",
                 "medicamentosUsoContinuo":["varfarina"],"alergias":["dipirona"]}""".formatted(nome, cpf);
    }

    static String mascarar(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    long criar(String cpf) throws Exception {
        String resp = mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content(json("Maria", cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resp, "$.id")).longValue();
    }

    @Test
    void cadastraEBuscaPorCpfComMascara() throws Exception {
        String cpf = CpfsTeste.novo();
        long id = criar(mascarar(cpf));
        mvc.perform(get("/api/pacientes").param("cpf", cpf).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].cpf").value(cpf))
                .andExpect(jsonPath("$[0].medicamentosUsoContinuo[0]").value("varfarina"))
                .andExpect(jsonPath("$[0].consentimentoIa").value(false));
        mvc.perform(get("/api/pacientes/{id}", id).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void buscaPorNomeParcialSemCaixa() throws Exception {
        String cpf = CpfsTeste.novo();
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content(json("Joaquim Zebedeu", cpf)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/pacientes").param("nome", "zebed").header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(jsonPath("$[0].cpf").value(cpf));
    }

    @Test
    void cpfDuplicadoMesmoComMascaraDiferente422() throws Exception {
        String cpf = CpfsTeste.novo();
        criar(cpf);
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content(json("Outra", mascarar(cpf))))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void cpfInvalido422() throws Exception {
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content(json("X", "111.111.111-11")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("CPF inválido"));
    }

    @Test
    void pacienteInexistente404() throws Exception {
        mvc.perform(get("/api/pacientes/{id}", 999999).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void consentimentoRevogadoPrevalece() throws Exception {
        long id = criar(CpfsTeste.novo());
        mvc.perform(put("/api/pacientes/{id}/consentimento-ia", id).header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":true}"))
                .andExpect(jsonPath("$.consentimentoIa").value(true));
        mvc.perform(put("/api/pacientes/{id}/consentimento-ia", id).header("Authorization", bearer(Perfil.ENFERMEIRO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":false}"))
                .andExpect(jsonPath("$.consentimentoIa").value(false));
    }

    @Test
    void gestorNaoCadastraPaciente403() throws Exception {
        mvc.perform(post("/api/pacientes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content(json("X", CpfsTeste.novo())))
                .andExpect(status().isForbidden());
    }

    @Test
    void escritasGeramAuditoria() throws Exception {
        long id = criar(CpfsTeste.novo());
        mvc.perform(put("/api/pacientes/{id}/consentimento-ia", id).header("Authorization", bearer(Perfil.ENFERMEIRO))
                .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":true}"));
        assertThat(logs.findAll()).filteredOn(l -> "Paciente".equals(l.getRecurso()) && String.valueOf(id).equals(l.getIdRecurso()))
                .extracting(l -> l.getAcao())
                .contains("CRIAR", "CONSENTIMENTO_IA")
                .allSatisfy(a -> assertThat(a).isNotNull());
        assertThat(logs.findAll()).filteredOn(l -> String.valueOf(id).equals(l.getIdRecurso()) && "CRIAR".equals(l.getAcao()))
                .allSatisfy(l -> assertThat(l.getUsuario()).isEqualTo("enfermeiro@teste.local"));
    }
}
