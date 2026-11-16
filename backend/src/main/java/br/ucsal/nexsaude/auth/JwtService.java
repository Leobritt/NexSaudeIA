package br.ucsal.nexsaude.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite e valida tokens JWT (HS256). */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMin;

    public JwtService(@Value("${nexsaude.jwt.segredo}") String segredo,
                      @Value("${nexsaude.jwt.expiracao-min}") long expiracaoMin) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMin = expiracaoMin;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm(agora)))
                .signWith(chave)
                .compact();
    }

    public Instant expiraEm(Instant emitidoEm) {
        return emitidoEm.plusSeconds(expiracaoMin * 60);
    }

    /** E-mail do titular, se o token for válido e não estiver expirado. */
    public Optional<String> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(chave).build()
                    .parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
