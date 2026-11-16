package br.ucsal.nexsaude.leito.dto;

import br.ucsal.nexsaude.leito.StatusLeito;
import br.ucsal.nexsaude.paciente.dto.PacienteResumoDto;

public record LeitoDto(Long id, String codigo, Long setorId, StatusLeito status, PacienteResumoDto pacienteAtual) {}
