package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.common.RegraNegocioException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransicaoLeitoTest {

    @ParameterizedTest
    @CsvSource({
            "LIVRE,OCUPADO", "LIVRE,RESERVADO", "LIVRE,INTERDITADO",
            "RESERVADO,OCUPADO", "RESERVADO,LIVRE",
            "OCUPADO,HIGIENIZACAO",
            "HIGIENIZACAO,LIVRE",
            "INTERDITADO,HIGIENIZACAO"})
    void permitidas(StatusLeito de, StatusLeito para) {
        assertThatCode(() -> TransicaoLeito.validar(de, para)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "HIGIENIZACAO,OCUPADO", "OCUPADO,LIVRE", "OCUPADO,RESERVADO", "INTERDITADO,LIVRE",
            "INTERDITADO,OCUPADO", "HIGIENIZACAO,RESERVADO", "LIVRE,HIGIENIZACAO", "LIVRE,LIVRE"})
    void proibidas(StatusLeito de, StatusLeito para) {
        assertThatThrownBy(() -> TransicaoLeito.validar(de, para))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining(de.name()).hasMessageContaining(para.name());
    }
}
