package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.leito.dto.InternacaoDto;
import org.springframework.stereotype.Service;

@Service
public class InternacaoService {

    public InternacaoDto internar(Long atendimentoId, Long leitoId) { throw new UnsupportedOperationException(); }

    public InternacaoDto transferir(Long id, Long leitoDestinoId) { throw new UnsupportedOperationException(); }

    public InternacaoDto alta(Long id, String motivo) { throw new UnsupportedOperationException(); }
}
