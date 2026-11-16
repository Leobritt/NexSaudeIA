package br.ucsal.nexsaude.paciente.dto;

import jakarta.validation.constraints.NotNull;

public record ConsentimentoRequest(@NotNull Boolean concedido) {}
