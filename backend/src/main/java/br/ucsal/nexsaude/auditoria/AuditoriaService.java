package br.ucsal.nexsaude.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Map;

/** Registra e consulta a trilha de auditoria. */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository repo;
    private final TransactionTemplate novaTransacao;

    public AuditoriaService(LogAuditoriaRepository repo, PlatformTransactionManager txManager) {
        this.repo = repo;
        this.novaTransacao = new TransactionTemplate(txManager);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Registra uma entrada. Dentro de uma transação de negócio, o registro só é gravado depois
     * do commit: operação desfeita não deixa rastro falso no log imutável. Fora de transação,
     * grava na hora.
     */
    public void registrar(String acao, String recurso, Object idRecurso, Map<String, Object> detalhes) {
        String id = idRecurso == null ? null : idRecurso.toString();
        LogAuditoria entrada = new LogAuditoria(usuarioAtual(), acao, recurso, id, detalhes);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gravar(entrada);
                }
            });
        } else {
            gravar(entrada);
        }
    }

    private void gravar(LogAuditoria entrada) {
        novaTransacao.executeWithoutResult(s -> repo.save(entrada));
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
