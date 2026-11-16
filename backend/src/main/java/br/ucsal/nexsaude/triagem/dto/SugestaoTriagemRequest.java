package br.ucsal.nexsaude.triagem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SugestaoTriagemRequest(
        @NotNull Long atendimentoId,
        @NotBlank @Schema(example = "Dor no peito há 2 horas, falta de ar") String sintomas,
        @NotNull @Valid SinaisVitais sinais,
        @Schema(description = "Risco iminente de vida: permite usar a IA sem consentimento (RN-002). Opcional, padrão false") Boolean riscoIminente) {

    public boolean emRiscoIminente() {
        return Boolean.TRUE.equals(riscoIminente);
    }
}
