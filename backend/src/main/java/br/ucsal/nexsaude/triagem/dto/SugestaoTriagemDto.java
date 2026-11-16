package br.ucsal.nexsaude.triagem.dto;

import br.ucsal.nexsaude.triagem.CorRisco;
import io.swagger.v3.oas.annotations.media.Schema;

/** Sugestão da IA. Nunca é gravada como triagem até o enfermeiro confirmar (RN-001). */
public record SugestaoTriagemDto(
        Long sugestaoId,
        CorRisco cor,
        Double confianca,
        String justificativa,
        @Schema(example = "SIMULADA") String implementacao,
        @Schema(description = "true quando a IA não respondeu: o front deve oferecer a triagem manual") boolean indisponivel,
        String motivo) {}
