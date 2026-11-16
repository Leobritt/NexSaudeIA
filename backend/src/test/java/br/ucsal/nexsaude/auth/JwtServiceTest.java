package br.ucsal.nexsaude.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes-0123456789";

    @Test
    void tokenGeradoDevolveEmailEPerfil() {
        JwtService jwt = new JwtService(SEGREDO, 60);
        Usuario u = new Usuario("Ana", "ana@x.com", "hash", Perfil.MEDICO);
        String token = jwt.gerar(u);
        assertThat(jwt.validar(token)).contains("ana@x.com");
    }

    @Test
    void tokenAdulteradoRejeitado() {
        JwtService jwt = new JwtService(SEGREDO, 60);
        String token = jwt.gerar(new Usuario("Ana", "ana@x.com", "hash", Perfil.MEDICO));
        String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        assertThat(jwt.validar(adulterado)).isEmpty();
    }

    @Test
    void tokenDeOutroSegredoRejeitado() {
        String token = new JwtService(SEGREDO, 60).gerar(new Usuario("Ana", "ana@x.com", "h", Perfil.MEDICO));
        assertThat(new JwtService(SEGREDO + "-outro", 60).validar(token)).isEmpty();
    }

    @Test
    void tokenExpiradoRejeitado() {
        JwtService jwt = new JwtService(SEGREDO, -1);
        String token = jwt.gerar(new Usuario("Ana", "ana@x.com", "h", Perfil.MEDICO));
        assertThat(jwt.validar(token)).isEmpty();
    }
}
