package br.ucsal.nexsaude.prescricao.dto;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;

import java.util.List;

public record AlertaDto(List<String> medicamentos, GravidadeInteracao gravidade, String descricao, String recomendacao) {}
