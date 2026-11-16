package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.common.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void normalizaMascara() {
        assertThat(Cpf.normalizar("123.456.789-09")).isEqualTo("12345678909");
        assertThat(Cpf.normalizar(" 12345678909 ")).isEqualTo("12345678909");
    }

    @ParameterizedTest
    @ValueSource(strings = {"111.111.111-11", "123", "123.456.789-00", "abc.def.ghi-jk", ""})
    void invalidos(String cpf) {
        assertThatThrownBy(() -> Cpf.normalizar(cpf))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void nuloInvalido() {
        assertThatThrownBy(() -> Cpf.normalizar(null)).isInstanceOf(RegraNegocioException.class);
    }
}
