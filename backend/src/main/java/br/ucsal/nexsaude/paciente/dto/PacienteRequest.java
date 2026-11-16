package br.ucsal.nexsaude.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PacienteRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Maria da Silva") String nome,
        @NotBlank @Size(max = 20) @Schema(example = "123.456.789-09") String cpf,
        @Size(max = 15) @Schema(example = "898001160535009") String cns,
        @NotNull @Past @Schema(example = "1980-05-17") LocalDate dataNascimento,
        @NotBlank @Pattern(regexp = "[FMO]", message = "deve ser F, M ou O") @Schema(example = "F", allowableValues = {"F", "M", "O"}) String sexo,
        @Schema(example = "[\"varfarina\"]") List<String> medicamentosUsoContinuo,
        @Schema(example = "[\"dipirona\"]") List<String> alergias) {}
