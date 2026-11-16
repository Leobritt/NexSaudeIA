package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.IntegrationTest;
import br.ucsal.nexsaude.auth.Perfil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternacaoIT extends IntegrationTest {

    @Autowired SetorRepository setores;
    @Autowired LeitoRepository leitos;

    Setor setor() {
        return setores.save(new Setor("Clínica " + UUID.randomUUID().toString().substring(0, 6), "ENFERMARIA"));
    }

    Leito leito(Setor s, StatusLeito status) {
        return leitos.save(new Leito("L-" + UUID.randomUUID().toString().substring(0, 8), s, status));
    }

    /** Atendimento pronto para internar no setor. */
    long aguardandoLeito(Setor s) throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "AMARELO");
        acaoMedica(at, "iniciar", null);
        acaoMedica(at, "indicar-internacao", "{\"setorId\":" + s.getId() + "}");
        return at;
    }

    String internar(long atendimentoId, long leitoId) throws Exception {
        return postJson("/api/internacoes", Perfil.GESTOR_LEITOS,
                "{\"atendimentoId\":" + atendimentoId + ",\"leitoId\":" + leitoId + "}");
    }

    StatusLeito statusDe(Leito l) {
        return leitos.findById(l.getId()).orElseThrow().getStatus();
    }

    @Test
    void alocarExigeAtendimentoAguardandoLeito() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.LIVRE);
        long at = abrirAtendimento(novoPaciente(false));
        mvc.perform(post("/api/internacoes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"atendimentoId\":" + at + ",\"leitoId\":" + l.getId() + "}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(statusDe(l)).isEqualTo(StatusLeito.LIVRE);
    }

    @Test
    void alocacaoOcupaLeitoEInternaAtendimento() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.LIVRE);
        long at = aguardandoLeito(s);
        String resp = internar(at, l.getId());
        assertThat((String) JsonPath.read(resp, "$.leitoCodigo")).isEqualTo(l.getCodigo());
        assertThat(statusDe(l)).isEqualTo(StatusLeito.OCUPADO);
        mvc.perform(get("/api/atendimentos/aguardando-leito").header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(at)).isEmpty());
    }

    @Test
    void alocarLeitoNaoLivre422() throws Exception {
        Setor s = setor();
        for (StatusLeito st : List.of(StatusLeito.OCUPADO, StatusLeito.HIGIENIZACAO, StatusLeito.INTERDITADO)) {
            Leito l = leito(s, st);
            long at = aguardandoLeito(s);
            mvc.perform(post("/api/internacoes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"atendimentoId\":" + at + ",\"leitoId\":" + l.getId() + "}"))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Test
    void leitoReservadoPodeSerOcupado() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.RESERVADO);
        internar(aguardandoLeito(s), l.getId());
        assertThat(statusDe(l)).isEqualTo(StatusLeito.OCUPADO);
    }

    @Test
    void alocacaoConcorrenteNoMesmoLeitoSoUmaVence() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.LIVRE);
        long a1 = aguardandoLeito(s);
        long a2 = aguardandoLeito(s);
        String token = bearer(Perfil.GESTOR_LEITOS);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Integer> tentar1 = () -> { largada.await(); return tentar(token, a1, l.getId()); };
        Callable<Integer> tentar2 = () -> { largada.await(); return tentar(token, a2, l.getId()); };
        Future<Integer> f1 = pool.submit(tentar1);
        Future<Integer> f2 = pool.submit(tentar2);
        largada.countDown();
        List<Integer> codigos = List.of(f1.get(30, TimeUnit.SECONDS), f2.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(codigos).containsOnlyOnce(201);
        assertThat(codigos).anyMatch(c -> c == 409 || c == 422);
        mvc.perform(get("/api/leitos").param("setor", s.getId().toString()).header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andExpect(jsonPath("$[0].status").value("OCUPADO"))
                .andExpect(jsonPath("$[0].pacienteAtual.id").exists());
    }

    int tentar(String token, long atendimentoId, long leitoId) throws Exception {
        return mvc.perform(post("/api/internacoes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"atendimentoId\":" + atendimentoId + ",\"leitoId\":" + leitoId + "}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void transferenciaLevaOrigemParaHigienizacao() throws Exception {
        Setor s = setor();
        Leito origem = leito(s, StatusLeito.LIVRE);
        Leito destino = leito(s, StatusLeito.LIVRE);
        long internacao = id(internar(aguardandoLeito(s), origem.getId()));
        mvc.perform(post("/api/internacoes/{id}/transferencia", internacao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"leitoDestinoId\":" + destino.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leitoId").value(destino.getId()));
        assertThat(statusDe(origem)).isEqualTo(StatusLeito.HIGIENIZACAO);
        assertThat(statusDe(destino)).isEqualTo(StatusLeito.OCUPADO);
    }

    @Test
    void transferirParaLeitoOcupado422() throws Exception {
        Setor s = setor();
        Leito origem = leito(s, StatusLeito.LIVRE);
        Leito ocupado = leito(s, StatusLeito.OCUPADO);
        long internacao = id(internar(aguardandoLeito(s), origem.getId()));
        mvc.perform(post("/api/internacoes/{id}/transferencia", internacao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"leitoDestinoId\":" + ocupado.getId() + "}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(statusDe(origem)).isEqualTo(StatusLeito.OCUPADO);
    }

    @Test
    void altaLiberaParaHigienizacaoEFinalizaAtendimento() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.LIVRE);
        long at = aguardandoLeito(s);
        long internacao = id(internar(at, l.getId()));
        mvc.perform(post("/api/internacoes/{id}/alta", internacao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"melhora clínica\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saida").exists())
                .andExpect(jsonPath("$.motivoAlta").value("melhora clínica"));
        assertThat(statusDe(l)).isEqualTo(StatusLeito.HIGIENIZACAO);
        mvc.perform(post("/api/internacoes/{id}/alta", internacao).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"de novo\"}"))
                .andExpect(status().isUnprocessableContent());
        // RN-003: não dá para ocupar antes da higienização
        long outro = aguardandoLeito(s);
        mvc.perform(post("/api/internacoes").header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"atendimentoId\":" + outro + ",\"leitoId\":" + l.getId() + "}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void higienizacaoConcluidaVoltaParaLivre() throws Exception {
        Setor s = setor();
        Leito l = leito(s, StatusLeito.HIGIENIZACAO);
        mvc.perform(patch("/api/leitos/{id}/status", l.getId()).header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"LIVRE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVRE"));
    }

    @Test
    void patchNaoAceitaOcupado() throws Exception {
        Leito l = leito(setor(), StatusLeito.LIVRE);
        mvc.perform(patch("/api/leitos/{id}/status", l.getId()).header("Authorization", bearer(Perfil.GESTOR_LEITOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OCUPADO\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void mapaDeLeitosPorSetorESetores() throws Exception {
        Setor s = setor();
        Leito l1 = leito(s, StatusLeito.LIVRE);
        leito(s, StatusLeito.INTERDITADO);
        leito(setor(), StatusLeito.LIVRE);
        mvc.perform(get("/api/leitos").param("setor", s.getId().toString()).header("Authorization", bearer(Perfil.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == %d)].pacienteAtual".formatted(l1.getId())).value(org.hamcrest.Matchers.contains((Object) null)));
        mvc.perform(get("/api/setores").header("Authorization", bearer(Perfil.GESTOR_LEITOS)))
                .andExpect(jsonPath("$[?(@.id == %d)].nome".formatted(s.getId())).value(org.hamcrest.Matchers.contains(s.getNome())));
    }

    @Test
    void indicarInternacaoSetorInexistente404() throws Exception {
        long at = abrirAtendimento(novoPaciente(false));
        triar(at, "VERDE");
        acaoMedica(at, "iniciar", null);
        mvc.perform(post("/api/atendimentos/{id}/indicar-internacao", at).header("Authorization", bearer(Perfil.MEDICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"setorId\":999999}"))
                .andExpect(status().isNotFound());
    }
}
