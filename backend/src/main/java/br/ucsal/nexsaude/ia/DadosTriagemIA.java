package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.triagem.dto.SinaisVitais;

/** Entrada pseudonimizada da triagem: sem nome, sem CPF. */
public record DadosTriagemIA(int idade, String sexo, String sintomas, SinaisVitais sinais) {}
