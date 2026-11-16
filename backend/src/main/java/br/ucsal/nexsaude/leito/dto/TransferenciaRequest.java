package br.ucsal.nexsaude.leito.dto;

import jakarta.validation.constraints.NotNull;

public record TransferenciaRequest(@NotNull Long leitoDestinoId) {}
