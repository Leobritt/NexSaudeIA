package br.ucsal.nexsaude.prescricao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {
    List<Prescricao> findByInternacaoPacienteIdOrderByCriadaEmDesc(Long pacienteId);
}
