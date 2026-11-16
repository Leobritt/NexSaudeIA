package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.common.RegraNegocioException;

/** Normalização e validação de CPF (dígitos verificadores). */
public final class Cpf {

    private Cpf() {}

    /** Devolve só os 11 dígitos. Lança RegraNegocioException se o CPF for inválido. */
    public static String normalizar(String cpf) {
        if (cpf == null) throw invalido();
        String d = cpf.replaceAll("[.\\-\\s]", "");
        if (!d.matches("\\d{11}") || d.chars().distinct().count() == 1) throw invalido();
        if (digito(d, 9) != d.charAt(9) - '0' || digito(d, 10) != d.charAt(10) - '0') throw invalido();
        return d;
    }

    private static int digito(String d, int n) {
        int soma = 0;
        for (int i = 0; i < n; i++) soma += (d.charAt(i) - '0') * (n + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static RegraNegocioException invalido() {
        return new RegraNegocioException("CPF inválido");
    }
}
