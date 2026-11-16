package br.ucsal.nexsaude.triagem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SinaisVitais(
        @Min(0) @Max(300) @Schema(example = "120") Integer paSistolica,
        @Min(0) @Max(250) @Schema(example = "80") Integer paDiastolica,
        @DecimalMin("25.0") @DecimalMax("45.0") @Schema(example = "36.8") Double temperatura,
        @Min(0) @Max(100) @Schema(example = "97") Integer spo2,
        @Min(0) @Max(300) @Schema(example = "88") Integer fc,
        @Min(0) @Max(80) @Schema(example = "16") Integer fr) {}
