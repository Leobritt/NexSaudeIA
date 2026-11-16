package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.prescricao.GravidadeInteracao;

import java.util.List;

public record InteracaoIA(List<String> medicamentos, GravidadeInteracao gravidade, String descricao, String recomendacao) {}
