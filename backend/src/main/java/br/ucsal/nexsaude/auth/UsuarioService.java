package br.ucsal.nexsaude.auth;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.dto.LoginResponse;
import br.ucsal.nexsaude.auth.dto.NovoUsuarioRequest;
import br.ucsal.nexsaude.common.RegraNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt, AuditoriaService auditoria) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String senha) {
        Usuario u = repo.findByEmail(normalizar(email))
                .filter(Usuario::isAtivo)
                .filter(x -> encoder.matches(senha, x.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        auditoria.registrar("LOGIN", "Usuario", u.getId(), Map.of("email", u.getEmail()));
        return new LoginResponse(jwt.gerar(u), u.getNome(), u.getPerfil(), jwt.expiraEm(Instant.now()));
    }

    @Transactional
    public Usuario criar(NovoUsuarioRequest req) {
        String email = normalizar(req.email());
        if (repo.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe usuário com o e-mail " + email);
        }
        Usuario u = repo.save(new Usuario(req.nome(), email, encoder.encode(req.senha()), req.perfil()));
        auditoria.registrar("CRIAR", "Usuario", u.getId(), Map.of("perfil", u.getPerfil().name()));
        return u;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repo.findAll();
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
