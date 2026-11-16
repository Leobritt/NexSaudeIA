package br.ucsal.nexsaude.auth;

import br.ucsal.nexsaude.auth.dto.LoginRequest;
import br.ucsal.nexsaude.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req.email(), req.senha());
    }
}
