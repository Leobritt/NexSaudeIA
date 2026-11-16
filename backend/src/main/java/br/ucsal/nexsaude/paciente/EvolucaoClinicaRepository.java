package br.ucsal.nexsaude.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvolucaoClinicaRepository extends JpaRepository<EvolucaoClinica, Long> {
    List<EvolucaoClinica> findByPacienteIdOrderByDataHoraDesc(Long pacienteId);
}
