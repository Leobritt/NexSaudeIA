package br.ucsal.nexsaude.leito.dto;

import br.ucsal.nexsaude.paciente.dto.PacienteResumoDto;

import java.time.OffsetDateTime;

public record InternacaoDto(Long id, Long atendimentoId, PacienteResumoDto paciente, Long leitoId, String leitoCodigo,
                            OffsetDateTime entrada, OffsetDateTime saida, String motivoAlta) {}
