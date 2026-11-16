package br.ucsal.nexsaude.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public record PacienteRequest(
        @NotBlank @Schema(example = "Maria da Silva") String nome,
        @NotBlank @Schema(example = "123.456.789-09") String cpf,
        @Schema(example = "898001160535009") String cns,
        @NotNull @Past @Schema(example = "1980-05-17") LocalDate dataNascimento,
        @NotBlank @Schema(example = "F", allowableValues = {"F", "M", "O"}) String sexo,
        @Schema(example = "[\"varfarina\"]") List<String> medicamentosUsoContinuo,
        @Schema(example = "[\"dipirona\"]") List<String> alergias) {}
