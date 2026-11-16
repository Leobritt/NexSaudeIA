package br.ucsal.nexsaude.prescricao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ItemPrescricaoDto(
        @NotBlank @Schema(example = "AAS") String medicamento,
        @NotBlank @Schema(example = "100 mg") String dose,
        @NotBlank @Schema(example = "VO") String via,
        @NotBlank @Schema(example = "1x ao dia") String frequencia) {}
