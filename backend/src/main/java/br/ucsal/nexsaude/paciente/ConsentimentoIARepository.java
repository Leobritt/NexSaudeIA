package br.ucsal.nexsaude.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimentoIARepository extends JpaRepository<ConsentimentoIA, Long> {
    Optional<ConsentimentoIA> findFirstByPacienteIdOrderByDataHoraDescIdDesc(Long pacienteId);
}
