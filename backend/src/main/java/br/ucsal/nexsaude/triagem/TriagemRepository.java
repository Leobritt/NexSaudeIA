package br.ucsal.nexsaude.triagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TriagemRepository extends JpaRepository<Triagem, Long> {

    List<Triagem> findByAtendimentoIdOrderByDataHoraAscIdAsc(Long atendimentoId);

    List<Triagem> findByAtendimentoPacienteIdOrderByDataHoraDesc(Long pacienteId);

    boolean existsByAtendimentoId(Long atendimentoId);
}
