package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.ia.ServicoIA;
import br.ucsal.nexsaude.ia.StatusSugestao;
import br.ucsal.nexsaude.ia.SugestaoIA;
import br.ucsal.nexsaude.ia.TipoSugestao;
import br.ucsal.nexsaude.triagem.dto.SugestaoTriagemDto;
import br.ucsal.nexsaude.triagem.dto.SugestaoTriagemRequest;
import br.ucsal.nexsaude.triagem.dto.TriagemDto;
import br.ucsal.nexsaude.triagem.dto.TriagemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TriagemService {

    private final AtendimentoService atendimentos;
    private final TriagemRepository repo;
    private final ServicoIA ia;
    private final AuditoriaService auditoria;

    public TriagemService(AtendimentoService atendimentos, TriagemRepository repo, ServicoIA ia, AuditoriaService auditoria) {
        this.atendimentos = atendimentos;
        this.repo = repo;
        this.ia = ia;
        this.auditoria = auditoria;
    }

    /** RN-001: só gera a sugestão; nada vira triagem aqui. */
    @Transactional
    public SugestaoTriagemDto sugerir(SugestaoTriagemRequest req) {
        Atendimento a = atendimentos.buscar(req.atendimentoId());
        exigirTriavel(a);
        SugestaoIA s = ia.sugerirTriagem(a.getPaciente(), req.sintomas(), req.sinais(), req.emRiscoIminente());
        return new SugestaoTriagemDto(s.getId(), s.getCor(), s.getConfianca(), s.getJustificativa(),
                s.getImplementacao(), !s.isSucesso(), s.isSucesso() ? null : "IA indisponível: faça a triagem manual");
    }

    /** Grava a cor escolhida pelo enfermeiro. A primeira triagem coloca o paciente na fila médica. */
    @Transactional
    public TriagemDto gravar(TriagemRequest req) {
        Atendimento a = atendimentos.buscar(req.atendimentoId());
        exigirTriavel(a);

        OrigemCor origem = OrigemCor.MANUAL;
        if (req.sugestaoId() != null) {
            SugestaoIA s = ia.buscar(req.sugestaoId());
            if (s.getTipo() != TipoSugestao.TRIAGEM || !Objects.equals(s.getPacienteId(), a.getPaciente().getId())) {
                throw new RegraNegocioException("Sugestão não pertence a este paciente");
            }
            if (s.getStatus() != StatusSugestao.PENDENTE) {
                throw new RegraNegocioException("Sugestão já foi utilizada");
            }
            if (s.isSucesso()) {
                boolean igual = s.getCor() == req.cor();
                origem = igual ? OrigemCor.IA_CONFIRMADA : OrigemCor.IA_RECLASSIFICADA;
                ia.marcarDecisao(s.getId(), igual ? StatusSugestao.CONFIRMADA : StatusSugestao.AJUSTADA);
            } else {
                ia.marcarDecisao(s.getId(), StatusSugestao.REJEITADA);
            }
        }

        boolean primeira = a.getStatus() == StatusAtendimento.AGUARDANDO_TRIAGEM;
        Triagem t = repo.save(new Triagem(a, req.sintomas(), req.sinais(), req.cor(), origem, req.sugestaoId(),
                UsuarioLogado.atual()));
        a.setCorAtual(req.cor());
        if (primeira) atendimentos.mudarStatus(a, StatusAtendimento.AGUARDANDO_ATENDIMENTO);

        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("atendimentoId", a.getId());
        detalhes.put("cor", req.cor().name());
        detalhes.put("origem", origem.name());
        if (req.sugestaoId() != null) detalhes.put("sugestaoId", req.sugestaoId());
        auditoria.registrar(primeira ? "TRIAR" : "RECLASSIFICAR", "Triagem", t.getId(), detalhes);
        return dto(t);
    }

    @Transactional(readOnly = true)
    public List<TriagemDto> doPaciente(Long pacienteId) {
        return repo.findByAtendimentoPacienteIdOrderByDataHoraDesc(pacienteId).stream().map(this::dto).toList();
    }

    private static void exigirTriavel(Atendimento a) {
        if (a.getStatus() == StatusAtendimento.FINALIZADO || a.getStatus() == StatusAtendimento.INTERNADO) {
            throw new RegraNegocioException("Atendimento " + a.getStatus() + " não aceita triagem");
        }
    }

    private TriagemDto dto(Triagem t) {
        return new TriagemDto(t.getId(), t.getAtendimento().getId(), t.getCor(), t.getOrigem(), t.getSintomas(),
                t.sinais(), t.getEnfermeiro().getNome(), t.getDataHora());
    }
}
