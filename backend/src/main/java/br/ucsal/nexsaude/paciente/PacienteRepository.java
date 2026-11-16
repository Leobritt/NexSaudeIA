package br.ucsal.nexsaude.paciente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    @Query("select p from Paciente p where lower(p.nome) like lower(concat('%', :nome, '%')) order by p.nome")
    List<Paciente> buscarPorNome(String nome);

    List<Paciente> findTop50ByOrderByNomeAsc();
}
