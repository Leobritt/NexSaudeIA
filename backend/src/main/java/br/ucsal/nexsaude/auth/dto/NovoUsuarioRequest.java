package br.ucsal.nexsaude.auth.dto;

import br.ucsal.nexsaude.auth.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovoUsuarioRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "deve ter pelo menos 8 caracteres") String senha,
        @NotNull Perfil perfil) {}
