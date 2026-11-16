package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.leito.dto.InternacaoDto;
import br.ucsal.nexsaude.triagem.Atendimento;
import br.ucsal.nexsaude.triagem.AtendimentoService;
import br.ucsal.nexsaude.triagem.StatusAtendimento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InternacaoService {

    private final InternacaoRepository repo;
    private final MovimentacaoLeitoRepository movimentacoes;
    private final LeitoService leitos;
    private final AtendimentoService atendimentos;
    private final AuditoriaService auditoria;

    public InternacaoService(InternacaoRepository repo, MovimentacaoLeitoRepository movimentacoes, LeitoService leitos,
                             AtendimentoService atendimentos, AuditoriaService auditoria) {
        this.repo = repo;
        this.movimentacoes = movimentacoes;
        this.leitos = leitos;
        this.atendimentos = atendimentos;
        this.auditoria = auditoria;
    }

    /** UC-02: ocupa o leito e interna o paciente indicado. */
    @Transactional
    public InternacaoDto internar(Long atendimentoId, Long leitoId) {
        Atendimento a = atendimentos.buscar(atendimentoId);
        Leito l = leitos.buscar(leitoId);
        atendimentos.mudarStatus(a, StatusAtendimento.INTERNADO);
        leitos.mudar(l, StatusLeito.OCUPADO);
        Internacao i = repo.saveAndFlush(new Internacao(a, l));
        auditoria.registrar("INTERNAR", "Internacao", i.getId(), Map.of("atendimentoId", atendimentoId, "leitoId", leitoId));
        return dto(i);
    }

    @Transactional
    public InternacaoDto transferir(Long id, Long leitoDestinoId) {
        Internacao i = ativa(id);
        Leito origem = i.getLeito();
        Leito destino = leitos.buscar(leitoDestinoId);
        leitos.mudar(destino, StatusLeito.OCUPADO);
        leitos.mudar(origem, StatusLeito.HIGIENIZACAO);
        i.trocarLeito(destino);
        repo.saveAndFlush(i);
        movimentacoes.save(new MovimentacaoLeito(i.getId(), origem.getId(), destino.getId(), UsuarioLogado.atual().getEmail()));
        auditoria.registrar("TRANSFERIR", "Internacao", i.getId(), Map.of("de", origem.getId(), "para", destino.getId()));
        return dto(i);
    }

    @Transactional
    public InternacaoDto alta(Long id, String motivo) {
        Internacao i = ativa(id);
        i.darAlta(motivo);
        leitos.mudar(i.getLeito(), StatusLeito.HIGIENIZACAO);
        atendimentos.mudarStatus(i.getAtendimento(), StatusAtendimento.FINALIZADO);
        auditoria.registrar("ALTA", "Internacao", i.getId(), Map.of("motivo", motivo));
        return dto(i);
    }

    @Transactional(readOnly = true)
    public Internacao buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Internação", id));
    }

    @Transactional(readOnly = true)
    public List<InternacaoDto> doPaciente(Long pacienteId) {
        return repo.findByPacienteIdOrderByEntradaDesc(pacienteId).stream().map(InternacaoService::dto).toList();
    }

    private Internacao ativa(Long id) {
        Internacao i = buscar(id);
        if (!i.isAtiva()) throw new RegraNegocioException("Internação já encerrada");
        return i;
    }

    public static InternacaoDto dto(Internacao i) {
        return new InternacaoDto(i.getId(), i.getAtendimento().getId(), i.getPaciente().resumo(), i.getLeito().getId(),
                i.getLeito().getCodigo(), i.getEntrada(), i.getSaida(), i.getMotivoAlta());
    }
}
