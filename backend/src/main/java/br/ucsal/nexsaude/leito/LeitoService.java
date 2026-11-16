package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.leito.dto.LeitoDto;
import br.ucsal.nexsaude.leito.dto.SetorDto;
import br.ucsal.nexsaude.paciente.dto.PacienteResumoDto;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeitoService {

    private final SetorRepository setores;
    private final LeitoRepository leitos;
    private final InternacaoRepository internacoes;
    private final AuditoriaService auditoria;

    public LeitoService(SetorRepository setores, LeitoRepository leitos, InternacaoRepository internacoes,
                        AuditoriaService auditoria) {
        this.setores = setores;
        this.leitos = leitos;
        this.internacoes = internacoes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<SetorDto> setores() {
        return setores.findAll(Sort.by("nome")).stream()
                .map(s -> new SetorDto(s.getId(), s.getNome(), s.getTipo())).toList();
    }

    /** Mapa de leitos com o paciente de cada leito ocupado. */
    @Transactional(readOnly = true)
    public List<LeitoDto> leitos(Long setorId) {
        Map<Long, PacienteResumoDto> ocupantes = internacoes.findBySaidaIsNull().stream()
                .collect(Collectors.toMap(i -> i.getLeito().getId(), i -> i.getPaciente().resumo(), (a, b) -> a));
        List<Leito> lista = setorId == null ? leitos.findAllByOrderBySetorIdAscCodigoAsc()
                : leitos.findBySetorIdOrderByCodigo(setorId);
        return lista.stream().map(l -> dto(l, ocupantes.get(l.getId()))).toList();
    }

    /** Mudança manual (higienização concluída, interdição, reserva). Ocupar só pela internação. */
    @Transactional
    public LeitoDto alterarStatus(Long id, StatusLeito status) {
        if (status == StatusLeito.OCUPADO) {
            throw new RegraNegocioException("Leito só fica ocupado por meio de uma internação");
        }
        Leito l = buscar(id);
        mudar(l, status);
        return dto(l, null);
    }

    @Transactional(readOnly = true)
    public Leito buscar(Long id) {
        return leitos.findById(id).orElseThrow(() -> new NaoEncontradoException("Leito", id));
    }

    @Transactional(readOnly = true)
    public void exigirSetor(Long setorId) {
        if (!setores.existsById(setorId)) throw new NaoEncontradoException("Setor", setorId);
    }

    /** Aplica a transição validada e registra na auditoria. */
    @Transactional
    public void mudar(Leito l, StatusLeito novo) {
        StatusLeito anterior = l.getStatus();
        l.mudarPara(novo);
        leitos.saveAndFlush(l);
        auditoria.registrar("STATUS", "Leito", l.getId(), Map.of("de", anterior.name(), "para", novo.name()));
    }

    static LeitoDto dto(Leito l, PacienteResumoDto paciente) {
        return new LeitoDto(l.getId(), l.getCodigo(), l.getSetor().getId(), l.getStatus(), paciente);
    }
}
