package br.ucsal.nexsaude.auth;

import br.ucsal.nexsaude.auth.dto.NovoUsuarioRequest;
import br.ucsal.nexsaude.auth.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return service.listar().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto criar(@Valid @RequestBody NovoUsuarioRequest req) {
        return UsuarioDto.de(service.criar(req));
    }
}
