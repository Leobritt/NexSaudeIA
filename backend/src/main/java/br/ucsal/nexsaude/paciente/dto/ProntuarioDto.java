package br.ucsal.nexsaude.paciente.dto;

import br.ucsal.nexsaude.leito.dto.InternacaoDto;
import br.ucsal.nexsaude.prescricao.dto.PrescricaoDto;
import br.ucsal.nexsaude.triagem.dto.AtendimentoDto;
import br.ucsal.nexsaude.triagem.dto.TriagemDto;

import java.util.List;

/** Visão consolidada do paciente. As listas vêm vazias quando o perfil não pode vê-las (RN-005). */
public record ProntuarioDto(PacienteDto paciente, List<AtendimentoDto> atendimentos, List<TriagemDto> triagens,
                            List<InternacaoDto> internacoes, List<PrescricaoDto> prescricoes,
                            List<EvolucaoDto> evolucoes) {}
