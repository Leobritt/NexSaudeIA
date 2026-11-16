package br.ucsal.nexsaude.auth.dto;

import br.ucsal.nexsaude.auth.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovoUsuarioRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String senha,
        @NotNull Perfil perfil) {}
