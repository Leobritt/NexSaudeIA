package br.ucsal.nexsaude.leito.dto;

import jakarta.validation.constraints.NotNull;

public record NovaInternacaoRequest(@NotNull Long atendimentoId, @NotNull Long leitoId) {}
