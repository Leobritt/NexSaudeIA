package br.ucsal.nexsaude.triagem.dto;

import br.ucsal.nexsaude.paciente.dto.PacienteResumoDto;
import br.ucsal.nexsaude.triagem.CorRisco;
import br.ucsal.nexsaude.triagem.StatusAtendimento;

import java.time.OffsetDateTime;

public record AtendimentoDto(Long id, PacienteResumoDto paciente, OffsetDateTime chegada, StatusAtendimento status,
                             CorRisco corAtual, String medicoResponsavel, Long setorIndicadoId) {}
