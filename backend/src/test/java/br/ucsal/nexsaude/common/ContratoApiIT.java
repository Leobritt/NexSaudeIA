package br.ucsal.nexsaude.common;

import br.ucsal.nexsaude.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que o contrato combinado com o front (seção 9 do design) está publicado. */
class ContratoApiIT extends IntegrationTest {

    static final String[][] ROTAS = {
            {"post", "/api/auth/login"},
            {"get", "/api/usuarios"}, {"post", "/api/usuarios"},
            {"get", "/api/pacientes"}, {"post", "/api/pacientes"},
            {"get", "/api/pacientes/{id}"},
            {"put", "/api/pacientes/{id}/consentimento-ia"},
            {"get", "/api/pacientes/{id}/prontuario"},
            {"post", "/api/pacientes/{id}/evolucoes"},
            {"post", "/api/atendimentos"},
            {"get", "/api/atendimentos/fila"},
            {"get", "/api/atendimentos/aguardando-leito"},
            {"post", "/api/atendimentos/{id}/iniciar"},
            {"post", "/api/atendimentos/{id}/indicar-internacao"},
            {"post", "/api/atendimentos/{id}/finalizar"},
            {"post", "/api/triagens/sugestao"},
            {"post", "/api/triagens"},
            {"get", "/api/setores"},
            {"get", "/api/leitos"},
            {"patch", "/api/leitos/{id}/status"},
            {"post", "/api/internacoes"},
            {"post", "/api/internacoes/{id}/transferencia"},
            {"post", "/api/internacoes/{id}/alta"},
            {"post", "/api/prescricoes"},
            {"get", "/api/prescricoes/{id}"},
            {"put", "/api/prescricoes/{id}/itens"},
            {"post", "/api/prescricoes/{id}/verificar"},
            {"post", "/api/prescricoes/{id}/assinar"},
            {"get", "/api/auditoria"},
    };

    @Test
    void todasAsRotasDoContratoEstaoNoOpenApi() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Object>> paths = JsonPath.read(json, "$.paths");
        for (String[] rota : ROTAS) {
            assertThat(paths).as("rota %s", rota[1]).containsKey(rota[1]);
            assertThat(paths.get(rota[1])).as("%s %s", rota[0], rota[1]).containsKey(rota[0]);
        }
    }
}
