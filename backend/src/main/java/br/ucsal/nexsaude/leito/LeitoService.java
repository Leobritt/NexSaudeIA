package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.leito.dto.LeitoDto;
import br.ucsal.nexsaude.leito.dto.SetorDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeitoService {

    public List<SetorDto> setores() { throw new UnsupportedOperationException(); }

    public List<LeitoDto> leitos(Long setorId) { throw new UnsupportedOperationException(); }

    public LeitoDto alterarStatus(Long id, StatusLeito status) { throw new UnsupportedOperationException(); }
}
