package br.ucsal.nexsaude.leito.dto;

import jakarta.validation.constraints.NotBlank;

public record AltaRequest(@NotBlank String motivo) {}
