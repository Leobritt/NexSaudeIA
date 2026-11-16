package br.ucsal.nexsaude.prescricao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssinarRequest(
        @Schema(description = "Obrigatória com interação GRAVE ou IA indisponível (RN-004)") String justificativa) {}
