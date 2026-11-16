package br.ucsal.nexsaude.triagem.dto;

import jakarta.validation.constraints.NotNull;

public record IndicarInternacaoRequest(@NotNull Long setorId) {}
