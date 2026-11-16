package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.paciente.Paciente;
import br.ucsal.nexsaude.paciente.PacienteService;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fachada da IA para os módulos clínicos (ERS 5.3): checa consentimento (RN-002), pseudonimiza a entrada,
 * chama a IA, guarda a resposta e registra na auditoria. Falhas da IA nunca sobem como erro: viram
 * uma sugestão marcada como indisponível, e o fluxo segue manual.
 */
@Service
public class ServicoIA {

    static final String PROMPT_TRIAGEM = "triagem-v1";
    static final String PROMPT_INTERACAO = "interacao-v1";

    private static final Logger log = LoggerFactory.getLogger(ServicoIA.class);

    private final AssistenteClinicoIA ia;
    private final SugestaoIARepository repo;
    private final PacienteService pacientes;
    private final AuditoriaService auditoria;
    private final JsonMapper json;

    public ServicoIA(AssistenteClinicoIA ia, SugestaoIARepository repo, PacienteService pacientes,
                     AuditoriaService auditoria, JsonMapper json) {
        this.ia = ia;
        this.repo = repo;
        this.pacientes = pacientes;
        this.auditoria = auditoria;
        this.json = json;
    }

    @Transactional
    public SugestaoIA sugerirTriagem(Paciente p, String sintomas, SinaisVitais sinais, boolean riscoIminente) {
        boolean semConsentimento = exigirConsentimento(p, riscoIminente);
        DadosTriagemIA dados = new DadosTriagemIA(p.idade(), p.getSexo(), sintomas, sinais);
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("idade", dados.idade());
        entrada.put("sexo", dados.sexo());
        entrada.put("sintomas", sintomas);
        entrada.put("sinais", json.convertValue(sinais, Map.class));

        SugestaoIA s = new SugestaoIA(TipoSugestao.TRIAGEM, p.getId(), entrada, PROMPT_TRIAGEM,
                ia.modelo(), ia.nomeImplementacao(), riscoIminente);
        executar(s, () -> ia.sugerirTriagem(dados), r -> s.registrarTriagem(r, json.writeValueAsString(r)));
        return salvarERegistrar(s, semConsentimento);
    }

    @Transactional
    public SugestaoIA verificarInteracoes(Paciente p, List<String> prescritos) {
        exigirConsentimento(p, false);
        List<String> usoContinuo = p.getMedicamentosUsoContinuo();
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("prescritos", prescritos);
        entrada.put("usoContinuo", usoContinuo);

        SugestaoIA s = new SugestaoIA(TipoSugestao.INTERACAO, p.getId(), entrada, PROMPT_INTERACAO,
                ia.modelo(), ia.nomeImplementacao(), false);
        executar(s, () -> ia.verificarInteracoes(prescritos, usoContinuo),
                r -> s.registrarInteracoes(r, json.writeValueAsString(r)));
        return salvarERegistrar(s, false);
    }

    @Transactional
    public void marcarDecisao(Long sugestaoId, StatusSugestao status) {
        SugestaoIA s = buscar(sugestaoId);
        s.setStatus(status);
        auditoria.registrar("DECISAO_IA", "SugestaoIA", s.getId(), Map.of("status", status.name()));
    }

    @Transactional(readOnly = true)
    public SugestaoIA buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Sugestão de IA", id));
    }

    /** @return true quando a chamada segue sem consentimento, amparada no risco iminente */
    private boolean exigirConsentimento(Paciente p, boolean riscoIminente) {
        if (pacientes.temConsentimentoIa(p.getId())) return false;
        if (riscoIminente) return true;
        throw new RegraNegocioException("Paciente sem consentimento para uso de IA");
    }

    private <R> void executar(SugestaoIA s, Supplier<R> chamada, Consumer<R> sucesso) {
        try {
            sucesso.accept(chamada.get());
        } catch (IAIndisponivelException e) {
            log.warn("IA indisponível: {}", e.getMessage());
            s.registrarFalha(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado na IA", e);
            s.registrarFalha("Erro inesperado: " + e.getMessage());
        }
    }

    private SugestaoIA salvarERegistrar(SugestaoIA s, boolean semConsentimento) {
        repo.save(s);
        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("tipo", s.getTipo().name());
        detalhes.put("pacienteId", s.getPacienteId());
        detalhes.put("implementacao", s.getImplementacao());
        detalhes.put("modelo", s.getModelo());
        detalhes.put("versaoPrompt", s.getVersaoPrompt());
        detalhes.put("sucesso", s.isSucesso());
        if (semConsentimento) detalhes.put("baseLegal", "RISCO_IMINENTE_SEM_CONSENTIMENTO");
        if (!s.isSucesso()) detalhes.put("motivoFalha", s.getMotivoFalha());
        auditoria.registrar("CHAMADA_IA", "SugestaoIA", s.getId(), detalhes);
        return s;
    }
}
