package br.ucsal.nexsaude.paciente.dto;

import java.time.LocalDate;
import java.util.List;

public record PacienteDto(Long id, String nome, String cpf, String cns, LocalDate dataNascimento, String sexo,
                          List<String> medicamentosUsoContinuo, List<String> alergias, boolean consentimentoIa) {}
