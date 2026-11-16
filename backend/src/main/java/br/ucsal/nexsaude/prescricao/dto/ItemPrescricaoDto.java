package br.ucsal.nexsaude.prescricao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemPrescricaoDto(
        @NotBlank @Size(max = 150) @Schema(example = "AAS") String medicamento,
        @NotBlank @Size(max = 60) @Schema(example = "100 mg") String dose,
        @NotBlank @Size(max = 30) @Schema(example = "VO") String via,
        @NotBlank @Size(max = 60) @Schema(example = "1x ao dia") String frequencia) {}
