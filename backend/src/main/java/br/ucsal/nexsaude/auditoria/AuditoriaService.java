package br.ucsal.nexsaude.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/** Registra e consulta a trilha de auditoria. */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository repo;

    public AuditoriaService(LogAuditoriaRepository repo) {
        this.repo = repo;
    }

    /**
     * Grava uma entrada em transação própria, para o registro sobreviver mesmo
     * que a operação de negócio seja desfeita depois.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String recurso, Object idRecurso, Map<String, Object> detalhes) {
        String id = idRecurso == null ? null : idRecurso.toString();
        repo.save(new LogAuditoria(usuarioAtual(), acao, recurso, id, detalhes));
    }

    @Transactional(readOnly = true)
    public Page<LogAuditoria> buscar(String usuario, String recurso, OffsetDateTime de, OffsetDateTime ate, Pageable pageable) {
        Specification<LogAuditoria> spec = (root, q, cb) -> cb.conjunction();
        if (usuario != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("usuario"), usuario));
        if (recurso != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("recurso"), recurso));
        if (de != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("dataHora"), de));
        if (ate != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("dataHora"), ate));
        return repo.findAll(spec, pageable);
    }

    private static String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "sistema";
        }
        return auth.getName();
    }
}
