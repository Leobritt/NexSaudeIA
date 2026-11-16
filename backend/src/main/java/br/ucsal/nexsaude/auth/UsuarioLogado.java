package br.ucsal.nexsaude.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao usuário autenticado da requisição atual. */
public final class UsuarioLogado {

    private UsuarioLogado() {}

    public static Usuario atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) {
            return u;
        }
        throw new IllegalStateException("Nenhum usuário autenticado");
    }
}
