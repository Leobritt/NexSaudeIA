package br.ucsal.nexsaude.triagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    List<Atendimento> findByStatusOrderByChegadaAscIdAsc(StatusAtendimento status);

    boolean existsByPacienteIdAndStatusNot(Long pacienteId, StatusAtendimento status);

    List<Atendimento> findByPacienteIdOrderByChegadaDesc(Long pacienteId);
}
