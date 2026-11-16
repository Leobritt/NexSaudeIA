package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.triagem.CorRisco;

public record ResultadoTriagemIA(CorRisco cor, double confianca, String justificativa) {}
