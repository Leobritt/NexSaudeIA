package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.leito.dto.AlterarStatusLeitoRequest;
import br.ucsal.nexsaude.leito.dto.LeitoDto;
import br.ucsal.nexsaude.leito.dto.SetorDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LeitoController {

    private final LeitoService service;

    public LeitoController(LeitoService service) {
        this.service = service;
    }

    @GetMapping("/setores")
    @PreAuthorize("hasAnyRole('GESTOR_LEITOS','MEDICO')")
    public List<SetorDto> setores() {
        return service.setores();
    }

    /** Mapa de leitos (RF-05). O front consulta a cada 10 s. */
    @GetMapping("/leitos")
    @PreAuthorize("hasAnyRole('GESTOR_LEITOS','MEDICO')")
    public List<LeitoDto> leitos(@RequestParam(required = false) Long setor) {
        return service.leitos(setor);
    }

    @PatchMapping("/leitos/{id}/status")
    @PreAuthorize("hasRole('GESTOR_LEITOS')")
    public LeitoDto alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusLeitoRequest req) {
        return service.alterarStatus(id, req.status());
    }
}
