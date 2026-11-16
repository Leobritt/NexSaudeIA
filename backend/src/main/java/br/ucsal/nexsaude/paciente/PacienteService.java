package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.paciente.dto.PacienteDto;
import br.ucsal.nexsaude.paciente.dto.PacienteRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PacienteService {

    public PacienteDto criar(PacienteRequest req) { throw new UnsupportedOperationException(); }

    public List<PacienteDto> pesquisar(String cpf, String nome) { throw new UnsupportedOperationException(); }

    public PacienteDto detalhar(Long id) { throw new UnsupportedOperationException(); }

    public PacienteDto definirConsentimento(Long id, boolean concedido) { throw new UnsupportedOperationException(); }
}
