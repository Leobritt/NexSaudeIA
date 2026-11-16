package br.ucsal.nexsaude.prescricao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ItensRequest(@NotEmpty List<@Valid ItemPrescricaoDto> itens) {}
