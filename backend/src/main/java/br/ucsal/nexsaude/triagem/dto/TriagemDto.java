package br.ucsal.nexsaude.triagem.dto;

import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.OrigemCor;

import java.time.OffsetDateTime;

public record TriagemDto(Long id, Long atendimentoId, CorRisco cor, OrigemCor origem, String sintomas,
                         SinaisVitais sinais, String enfermeiro, OffsetDateTime dataHora) {}
