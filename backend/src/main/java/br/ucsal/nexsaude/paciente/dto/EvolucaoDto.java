package br.ucsal.nexsaude.paciente.dto;

import java.time.OffsetDateTime;

public record EvolucaoDto(Long id, String autor, String texto, OffsetDateTime dataHora) {}
