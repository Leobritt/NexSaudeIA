package br.ucsal.nexsaude.prescricao.dto;

import br.ucsal.nexsaude.prescricao.StatusPrescricao;
import br.ucsal.nexsaude.prescricao.StatusVerificacao;

import java.time.OffsetDateTime;
import java.util.List;

public record PrescricaoDto(Long id, Long internacaoId, StatusPrescricao status, StatusVerificacao statusVerificacao,
                            List<ItemPrescricaoDto> itens, List<AlertaDto> alertas, String medico,
                            String justificativa, OffsetDateTime assinadaEm, String hashAssinatura) {}
