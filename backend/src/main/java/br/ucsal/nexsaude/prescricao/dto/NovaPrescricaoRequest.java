package br.ucsal.nexsaude.prescricao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NovaPrescricaoRequest(@NotNull Long internacaoId, @NotEmpty List<@Valid ItemPrescricaoDto> itens) {}
