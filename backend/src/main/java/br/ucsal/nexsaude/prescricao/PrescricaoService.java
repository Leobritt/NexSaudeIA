package br.ucsal.nexsaude.prescricao;

import br.ucsal.nexsaude.prescricao.dto.ItemPrescricaoDto;
import br.ucsal.nexsaude.prescricao.dto.PrescricaoDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescricaoService {

    public PrescricaoDto criar(Long internacaoId, List<ItemPrescricaoDto> itens) { throw new UnsupportedOperationException(); }

    public PrescricaoDto detalhar(Long id) { throw new UnsupportedOperationException(); }

    public PrescricaoDto atualizarItens(Long id, List<ItemPrescricaoDto> itens) { throw new UnsupportedOperationException(); }

    public PrescricaoDto verificar(Long id) { throw new UnsupportedOperationException(); }

    public PrescricaoDto assinar(Long id, String justificativa) { throw new UnsupportedOperationException(); }
}
