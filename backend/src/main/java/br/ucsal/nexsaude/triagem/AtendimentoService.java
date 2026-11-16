package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.triagem.dto.AtendimentoDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtendimentoService {

    public AtendimentoDto abrir(Long pacienteId) { throw new UnsupportedOperationException(); }

    public List<AtendimentoDto> fila() { throw new UnsupportedOperationException(); }

    public List<AtendimentoDto> aguardandoLeito() { throw new UnsupportedOperationException(); }

    public AtendimentoDto iniciar(Long id) { throw new UnsupportedOperationException(); }

    public AtendimentoDto indicarInternacao(Long id, Long setorId) { throw new UnsupportedOperationException(); }

    public AtendimentoDto finalizar(Long id) { throw new UnsupportedOperationException(); }
}
