package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.common.NaoEncontradoException;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.paciente.dto.PacienteDto;
import br.ucsal.nexsaude.paciente.dto.PacienteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PacienteService {

    private final PacienteRepository repo;
    private final ConsentimentoIARepository consentimentos;
    private final AuditoriaService auditoria;

    public PacienteService(PacienteRepository repo, ConsentimentoIARepository consentimentos, AuditoriaService auditoria) {
        this.repo = repo;
        this.consentimentos = consentimentos;
        this.auditoria = auditoria;
    }

    @Transactional
    public PacienteDto criar(PacienteRequest req) {
        String cpf = Cpf.normalizar(req.cpf());
        if (repo.existsByCpf(cpf)) {
            throw new RegraNegocioException("Já existe paciente com este CPF");
        }
        Paciente p = repo.save(new Paciente(req.nome().trim(), cpf, req.cns(), req.dataNascimento(), req.sexo(),
                req.medicamentosUsoContinuo(), req.alergias()));
        auditoria.registrar("CRIAR", "Paciente", p.getId(), Map.of());
        return dto(p);
    }

    @Transactional(readOnly = true)
    public List<PacienteDto> pesquisar(String cpf, String nome) {
        List<Paciente> lista;
        if (cpf != null && !cpf.isBlank()) {
            lista = repo.findByCpf(Cpf.normalizar(cpf)).stream().toList();
        } else if (nome != null && !nome.isBlank()) {
            lista = repo.buscarPorNome(nome.trim());
        } else {
            lista = repo.findTop50ByOrderByNomeAsc();
        }
        return lista.stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public PacienteDto detalhar(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public PacienteDto definirConsentimento(Long id, boolean concedido) {
        Paciente p = buscar(id);
        consentimentos.save(new ConsentimentoIA(id, concedido, UsuarioLogado.atual().getEmail()));
        auditoria.registrar("CONSENTIMENTO_IA", "Paciente", id, Map.of("concedido", concedido));
        return dto(p);
    }

    /** Entidade do paciente, para os outros módulos. */
    @Transactional(readOnly = true)
    public Paciente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Paciente", id));
    }

    /** RN-002: vale o registro de consentimento mais recente; sem registro, não há consentimento. */
    @Transactional(readOnly = true)
    public boolean temConsentimentoIa(Long pacienteId) {
        return consentimentos.findFirstByPacienteIdOrderByDataHoraDescIdDesc(pacienteId)
                .map(ConsentimentoIA::isConcedido).orElse(false);
    }

    public PacienteDto dto(Paciente p) {
        return new PacienteDto(p.getId(), p.getNome(), p.getCpf(), p.getCns(), p.getDataNascimento(), p.getSexo(),
                p.getMedicamentosUsoContinuo(), p.getAlergias(), temConsentimentoIa(p.getId()));
    }
}
