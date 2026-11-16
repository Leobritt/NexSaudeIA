package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.triagem.dto.SugestaoTriagemDto;
import br.ucsal.nexsaude.triagem.dto.SugestaoTriagemRequest;
import br.ucsal.nexsaude.triagem.dto.TriagemDto;
import br.ucsal.nexsaude.triagem.dto.TriagemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/triagens")
@PreAuthorize("hasRole('ENFERMEIRO')")
public class TriagemController {

    private final TriagemService service;

    public TriagemController(TriagemService service) {
        this.service = service;
    }

    /** Pede à IA uma sugestão de cor. Não grava triagem (RN-001). */
    @PostMapping("/sugestao")
    public SugestaoTriagemDto sugerir(@Valid @RequestBody SugestaoTriagemRequest req) {
        return service.sugerir(req);
    }

    /** Grava a triagem definitiva com a cor escolhida pelo enfermeiro. Reclassificar = nova chamada (RN-007). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriagemDto gravar(@Valid @RequestBody TriagemRequest req) {
        return service.gravar(req);
    }
}
