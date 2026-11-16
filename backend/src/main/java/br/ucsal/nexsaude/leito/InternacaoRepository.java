package br.ucsal.nexsaude.leito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternacaoRepository extends JpaRepository<Internacao, Long> {
    List<Internacao> findBySaidaIsNull();

    List<Internacao> findByPacienteIdOrderByEntradaDesc(Long pacienteId);
}
