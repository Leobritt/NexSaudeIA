package br.ucsal.nexsaude.auth.dto;

import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.auth.Usuario;

public record UsuarioDto(Long id, String nome, String email, Perfil perfil, boolean ativo) {
    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo());
    }
}
