package br.ucsal.nexsaude;

import java.util.concurrent.ThreadLocalRandom;

/** Gera CPFs válidos e distintos para os testes. */
public final class CpfsTeste {

    private CpfsTeste() {}

    public static String novo() {
        int[] d = new int[11];
        ThreadLocalRandom r = ThreadLocalRandom.current();
        do {
            for (int i = 0; i < 9; i++) d[i] = r.nextInt(10);
        } while (d[0] == d[1] && d[1] == d[2]);
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    private static int digito(int[] d, int n) {
        int soma = 0;
        for (int i = 0; i < n; i++) soma += d[i] * (n + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
