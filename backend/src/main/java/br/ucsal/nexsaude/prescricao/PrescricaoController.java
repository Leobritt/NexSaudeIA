package br.ucsal.nexsaude.prescricao;

import br.ucsal.nexsaude.prescricao.dto.AssinarRequest;
import br.ucsal.nexsaude.prescricao.dto.ItensRequest;
import br.ucsal.nexsaude.prescricao.dto.NovaPrescricaoRequest;
import br.ucsal.nexsaude.prescricao.dto.PrescricaoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescricoes")
@PreAuthorize("hasRole('MEDICO')")
public class PrescricaoController {

    private final PrescricaoService service;

    public PrescricaoController(PrescricaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescricaoDto criar(@Valid @RequestBody NovaPrescricaoRequest req) {
        return service.criar(req.internacaoId(), req.itens());
    }

    @GetMapping("/{id}")
    public PrescricaoDto detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    /** Só em rascunho. Exige nova verificação de interações depois. */
    @PutMapping("/{id}/itens")
    public PrescricaoDto atualizarItens(@PathVariable Long id, @Valid @RequestBody ItensRequest req) {
        return service.atualizarItens(id, req.itens());
    }

    /** Verifica interações medicamentosas com a IA (RF-08). */
    @PostMapping("/{id}/verificar")
    public PrescricaoDto verificar(@PathVariable Long id) {
        return service.verificar(id);
    }

    /** Assina eletronicamente (RF-07). Bloqueia interação grave sem justificativa (RN-004). */
    @PostMapping("/{id}/assinar")
    public PrescricaoDto assinar(@PathVariable Long id, @RequestBody(required = false) AssinarRequest req) {
        return service.assinar(id, req == null ? null : req.justificativa());
    }
}
