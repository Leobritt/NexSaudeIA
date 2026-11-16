package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.paciente.PacienteService;
import br.ucsal.nexsaude.triagem.dto.AtendimentoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.ucsal.nexsaude.triagem.StatusAtendimento.*;

@Service
public class AtendimentoService {

    /** Transições permitidas (seção 6 do design). */
    private static final Map<StatusAtendimento, Set<StatusAtendimento>> TRANSICOES = Map.of(
            AGUARDANDO_TRIAGEM, EnumSet.of(AGUARDANDO_ATENDIMENTO),
            AGUARDANDO_ATENDIMENTO, EnumSet.of(EM_ATENDIMENTO),
            EM_ATENDIMENTO, EnumSet.of(AGUARDANDO_LEITO, FINALIZADO),
            AGUARDANDO_LEITO, EnumSet.of(INTERNADO),
            INTERNADO, EnumSet.of(FINALIZADO),
            FINALIZADO, EnumSet.noneOf(StatusAtendimento.class));

    private final AtendimentoRepository repo;
    private final PacienteService pacientes;
    private final AuditoriaService auditoria;

    public AtendimentoService(AtendimentoRepository repo, PacienteService pacientes, AuditoriaService auditoria) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.auditoria = auditoria;
    }

    @Transactional
    public AtendimentoDto abrir(Long pacienteId) {
        var paciente = pacientes.buscar(pacienteId);
        if (repo.existsByPacienteIdAndStatusNot(pacienteId, FINALIZADO)) {
            throw new RegraNegocioException("Paciente já tem um atendimento em andamento");
        }
        Atendimento a = repo.save(new Atendimento(paciente));
        auditoria.registrar("ABRIR", "Atendimento", a.getId(), Map.of("pacienteId", pacienteId));
        return dto(a);
    }

    /** Aguardando médico: cor mais grave primeiro, depois ordem de chegada. */
    @Transactional(readOnly = true)
    public List<AtendimentoDto> fila() {
        return repo.findByStatusOrderByChegadaAscIdAsc(AGUARDANDO_ATENDIMENTO).stream()
                .sorted(Comparator.comparing(Atendimento::getCorAtual)
                        .thenComparing(Atendimento::getChegada)
                        .thenComparing(Atendimento::getId))
                .map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<AtendimentoDto> aguardandoLeito() {
        return repo.findByStatusOrderByChegadaAscIdAsc(AGUARDANDO_LEITO).stream()
                .sorted(Comparator.comparing(Atendimento::getCorAtual).thenComparing(Atendimento::getChegada))
                .map(this::dto).toList();
    }

    @Transactional
    public AtendimentoDto iniciar(Long id) {
        Atendimento a = buscar(id);
        mudarStatus(a, EM_ATENDIMENTO);
        a.setMedicoResponsavel(UsuarioLogado.atual());
        return dto(a);
    }

    @Transactional
    public AtendimentoDto indicarInternacao(Long id, Long setorId) {
        Atendimento a = buscar(id);
        mudarStatus(a, AGUARDANDO_LEITO);
        a.setSetorIndicadoId(setorId);
        return dto(a);
    }

    @Transactional
    public AtendimentoDto finalizar(Long id) {
        Atendimento a = buscar(id);
        mudarStatus(a, FINALIZADO);
        return dto(a);
    }

    @Transactional(readOnly = true)
    public Atendimento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Atendimento", id));
    }

    /** Valida e aplica a transição; registra na auditoria. */
    @Transactional
    public void mudarStatus(Atendimento a, StatusAtendimento novo) {
        if (!TRANSICOES.get(a.getStatus()).contains(novo)) {
            throw new RegraNegocioException("Atendimento em " + a.getStatus() + " não pode ir para " + novo);
        }
        StatusAtendimento anterior = a.getStatus();
        a.setStatus(novo);
        repo.save(a);
        auditoria.registrar("STATUS", "Atendimento", a.getId(), Map.of("de", anterior.name(), "para", novo.name()));
    }

    @Transactional(readOnly = true)
    public List<AtendimentoDto> doPaciente(Long pacienteId) {
        return repo.findByPacienteIdOrderByChegadaDesc(pacienteId).stream().map(this::dto).toList();
    }

    public AtendimentoDto dto(Atendimento a) {
        return new AtendimentoDto(a.getId(), a.getPaciente().resumo(), a.getChegada(), a.getStatus(), a.getCorAtual(),
                a.getMedicoResponsavel() == null ? null : a.getMedicoResponsavel().getNome(), a.getSetorIndicadoId());
    }
}
