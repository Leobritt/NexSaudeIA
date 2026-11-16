package br.ucsal.nexsaude.paciente.dto;

import java.time.LocalDate;

public record PacienteResumoDto(Long id, String nome, String cpf, LocalDate dataNascimento) {}
