package br.ucsal.nexsaude;

import br.ucsal.nexsaude.auth.JwtService;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.auth.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Locale;

/** Base dos testes de integração: sobe um PostgreSQL 16 real, compartilhado entre as classes. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTest {

    public static final String SENHA_TESTE = "senha123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    /** Usuário de teste do perfil (criado na primeira vez), e-mail perfil@teste.local. */
    protected Usuario usuario(Perfil perfil) {
        String email = perfil.name().toLowerCase(Locale.ROOT) + "@teste.local";
        return usuarioRepository.findByEmail(email).orElseGet(() -> usuarioRepository.save(
                new Usuario("Teste " + perfil.name(), email, passwordEncoder.encode(SENHA_TESTE), perfil)));
    }

    /** Header Authorization pronto para o perfil. */
    protected String bearer(Perfil perfil) {
        return "Bearer " + jwtService.gerar(usuario(perfil));
    }
}
