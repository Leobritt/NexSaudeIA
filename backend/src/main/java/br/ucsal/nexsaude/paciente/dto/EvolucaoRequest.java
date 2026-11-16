package br.ucsal.nexsaude.paciente.dto;

import jakarta.validation.constraints.NotBlank;

public record EvolucaoRequest(@NotBlank String texto) {}
