package br.ucsal.nexsaude.prescricao;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.ia.ServicoIA;
import br.ucsal.nexsaude.ia.SugestaoIA;
import br.ucsal.nexsaude.leito.Internacao;
import br.ucsal.nexsaude.leito.InternacaoService;
import br.ucsal.nexsaude.paciente.Paciente;
import br.ucsal.nexsaude.paciente.PacienteService;
import br.ucsal.nexsaude.prescricao.dto.AlertaDto;
import br.ucsal.nexsaude.prescricao.dto.ItemPrescricaoDto;
import br.ucsal.nexsaude.prescricao.dto.PrescricaoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescricaoService {

    private final PrescricaoRepository repo;
    private final InternacaoService internacoes;
    private final ServicoIA ia;
    private final AuditoriaService auditoria;
    private final JsonMapper json;
    private final PacienteService pacientes;

    public PrescricaoService(PrescricaoRepository repo, InternacaoService internacoes, ServicoIA ia,
                             AuditoriaService auditoria, JsonMapper json, PacienteService pacientes) {
        this.pacientes = pacientes;
        this.repo = repo;
        this.internacoes = internacoes;
        this.ia = ia;
        this.auditoria = auditoria;
        this.json = json;
    }

    @Transactional
    public PrescricaoDto criar(Long internacaoId, List<ItemPrescricaoDto> itens) {
        Internacao i = internacoes.buscar(internacaoId);
        if (!i.isAtiva()) throw new RegraNegocioException("Internação encerrada não recebe prescrição");
        Prescricao p = repo.save(new Prescricao(i, UsuarioLogado.atual(), paraItens(itens)));
        auditoria.registrar("CRIAR", "Prescricao", p.getId(), Map.of("internacaoId", internacaoId));
        return dto(p);
    }

    @Transactional(readOnly = true)
    public PrescricaoDto detalhar(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public PrescricaoDto atualizarItens(Long id, List<ItemPrescricaoDto> itens) {
        Prescricao p = buscar(id);
        p.trocarItens(paraItens(itens));
        auditoria.registrar("EDITAR", "Prescricao", id, Map.of("itens", itens.size()));
        return dto(p);
    }

    /** RF-08. Sem consentimento ou com a IA fora do ar, marca IA_INDISPONIVEL em vez de falhar. */
    @Transactional
    public PrescricaoDto verificar(Long id) {
        Prescricao p = buscar(id);
        if (p.getStatus() != StatusPrescricao.RASCUNHO) {
            throw new RegraNegocioException("Prescrição assinada não pode ser alterada");
        }
        List<String> nomes = p.getItens().stream().map(ItemPrescricao::getMedicamento).toList();
        Paciente paciente = p.getInternacao().getPaciente();
        if (!pacientes.temConsentimentoIa(paciente.getId())) {
            p.registrarVerificacao(StatusVerificacao.IA_INDISPONIVEL, List.of(), null);
            auditoria.registrar("VERIFICAR", "Prescricao", id, Map.of("resultado", "SEM_CONSENTIMENTO"));
            return dto(p);
        }
        SugestaoIA s = ia.verificarInteracoes(paciente, nomes);
        if (s.isSucesso()) {
            List<AlertaInteracao> alertas = s.getInteracoes().stream()
                    .map(x -> new AlertaInteracao(x.medicamentos(), x.gravidade(), x.descricao(), x.recomendacao()))
                    .toList();
            p.registrarVerificacao(StatusVerificacao.VERIFICADA_IA, alertas, s.getId());
        } else {
            p.registrarVerificacao(StatusVerificacao.IA_INDISPONIVEL, List.of(), s.getId());
        }
        auditoria.registrar("VERIFICAR", "Prescricao", id,
                Map.of("resultado", p.getStatusVerificacao().name(), "alertas", p.getAlertas().size()));
        return dto(p);
    }

    /** RF-07 + RN-004. A "assinatura" é médico + data/hora + SHA-256 do conteúdo. */
    @Transactional
    public PrescricaoDto assinar(Long id, String justificativa) {
        Prescricao p = buscar(id);
        Usuario medico = UsuarioLogado.atual();
        OffsetDateTime agora = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        p.assinar(medico, justificativa, agora, hash(p, medico, agora));

        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("hash", p.getHashAssinatura());
        detalhes.put("interacaoGrave", p.temInteracaoGrave());
        detalhes.put("semVerificacaoIa", p.getStatusVerificacao() == StatusVerificacao.IA_INDISPONIVEL);
        if (p.getJustificativa() != null) detalhes.put("justificativa", p.getJustificativa());
        auditoria.registrar("ASSINAR", "Prescricao", id, detalhes);
        return dto(p);
    }

    @Transactional(readOnly = true)
    public List<PrescricaoDto> doPaciente(Long pacienteId) {
        return repo.findByInternacaoPacienteIdOrderByCriadaEmDesc(pacienteId).stream().map(this::dto).toList();
    }

    private Prescricao buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Prescrição", id));
    }

    /** JSON canônico (ordem fixa de campos) do que foi assinado. */
    private String hash(Prescricao p, Usuario medico, OffsetDateTime quando) {
        Map<String, Object> conteudo = new LinkedHashMap<>();
        conteudo.put("internacaoId", p.getInternacao().getId());
        conteudo.put("medicoId", medico.getId());
        conteudo.put("assinadaEm", quando.toInstant().toString());
        conteudo.put("itens", p.getItens().stream().map(i -> List.of(i.getMedicamento(), i.getDose(), i.getVia(), i.getFrequencia())).toList());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsString(conteudo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<ItemPrescricao> paraItens(List<ItemPrescricaoDto> itens) {
        return itens.stream().map(i -> new ItemPrescricao(i.medicamento(), i.dose(), i.via(), i.frequencia())).toList();
    }

    private PrescricaoDto dto(Prescricao p) {
        return new PrescricaoDto(p.getId(), p.getInternacao().getId(), p.getStatus(), p.getStatusVerificacao(),
                p.getItens().stream().map(i -> new ItemPrescricaoDto(i.getMedicamento(), i.getDose(), i.getVia(), i.getFrequencia())).toList(),
                p.getAlertas().stream().map(a -> new AlertaDto(a.getMedicamentos(), a.getGravidade(), a.getDescricao(), a.getRecomendacao())).toList(),
                p.getMedico().getNome(), p.getJustificativa(), p.getAssinadaEm(), p.getHashAssinatura());
    }
}
