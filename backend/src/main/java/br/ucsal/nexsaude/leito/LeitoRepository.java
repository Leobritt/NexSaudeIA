package br.ucsal.nexsaude.leito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeitoRepository extends JpaRepository<Leito, Long> {
    List<Leito> findBySetorIdOrderByCodigo(Long setorId);

    List<Leito> findAllByOrderBySetorIdAscCodigoAsc();
}
