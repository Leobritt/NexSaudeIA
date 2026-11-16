package br.ucsal.nexsaude.auth.dto;

import br.ucsal.nexsaude.auth.Perfil;

import java.time.Instant;

public record LoginResponse(String token, String nome, Perfil perfil, Instant expiraEm) {}
