package br.ucsal.nexsaude.auditoria;

import br.ucsal.nexsaude.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditoriaServiceIT extends IntegrationTest {

    @Autowired AuditoriaService auditoria;
    @Autowired LogAuditoriaRepository repo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void registraUm() {
        auditoria.registrar("CRIAR", "Paciente", 42L, Map.of("campo", "valor"));
    }

    @Test
    void registrarGravaEntrada() {
        assertThat(repo.findAll())
                .anySatisfy(l -> {
                    assertThat(l.getAcao()).isEqualTo("CRIAR");
                    assertThat(l.getRecurso()).isEqualTo("Paciente");
                    assertThat(l.getIdRecurso()).isEqualTo("42");
                    assertThat(l.getUsuario()).isEqualTo("sistema");
                    assertThat(l.getDetalhes()).containsEntry("campo", "valor");
                });
    }

    @Test
    void updateDiretoNoBancoFalha() {
        assertThatThrownBy(() -> jdbc.update("update log_auditoria set acao = 'x'"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutável");
    }

    @Test
    void deleteDiretoNoBancoFalha() {
        assertThatThrownBy(() -> jdbc.update("delete from log_auditoria"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutável");
    }
}
