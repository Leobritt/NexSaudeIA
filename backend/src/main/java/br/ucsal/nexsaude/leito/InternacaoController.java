package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.leito.dto.AltaRequest;
import br.ucsal.nexsaude.leito.dto.InternacaoDto;
import br.ucsal.nexsaude.leito.dto.NovaInternacaoRequest;
import br.ucsal.nexsaude.leito.dto.TransferenciaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internacoes")
public class InternacaoController {

    private final InternacaoService service;

    public InternacaoController(InternacaoService service) {
        this.service = service;
    }

    /** Aloca o paciente num leito livre (UC-02). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GESTOR_LEITOS')")
    public InternacaoDto internar(@Valid @RequestBody NovaInternacaoRequest req) {
        return service.internar(req.atendimentoId(), req.leitoId());
    }

    @PostMapping("/{id}/transferencia")
    @PreAuthorize("hasAnyRole('GESTOR_LEITOS','MEDICO')")
    public InternacaoDto transferir(@PathVariable Long id, @Valid @RequestBody TransferenciaRequest req) {
        return service.transferir(id, req.leitoDestinoId());
    }

    @PostMapping("/{id}/alta")
    @PreAuthorize("hasAnyRole('GESTOR_LEITOS','MEDICO')")
    public InternacaoDto alta(@PathVariable Long id, @Valid @RequestBody AltaRequest req) {
        return service.alta(id, req.motivo());
    }
}
