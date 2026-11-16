package br.ucsal.nexsaude.triagem.dto;

import br.ucsal.nexsaude.triagem.CorRisco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Triagem definitiva, com a cor escolhida pelo enfermeiro. sugestaoId é opcional (triagem manual). */
public record TriagemRequest(@NotNull Long atendimentoId, @NotBlank String sintomas, @NotNull @Valid SinaisVitais sinais,
                             @NotNull CorRisco cor, Long sugestaoId) {}
