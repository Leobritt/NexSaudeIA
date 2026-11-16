package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.triagem.dto.AtendimentoDto;
import br.ucsal.nexsaude.triagem.dto.IndicarInternacaoRequest;
import br.ucsal.nexsaude.triagem.dto.NovoAtendimentoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    /** Chegada do paciente: abre o atendimento aguardando triagem. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENFERMEIRO')")
    public AtendimentoDto abrir(@Valid @RequestBody NovoAtendimentoRequest req) {
        return service.abrir(req.pacienteId());
    }

    /** Pacientes triados aguardando médico, ordenados por cor e depois por chegada. */
    @GetMapping("/fila")
    @PreAuthorize("hasAnyRole('ENFERMEIRO','MEDICO')")
    public List<AtendimentoDto> fila() {
        return service.fila();
    }

    @GetMapping("/aguardando-leito")
    @PreAuthorize("hasRole('GESTOR_LEITOS')")
    public List<AtendimentoDto> aguardandoLeito() {
        return service.aguardandoLeito();
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('MEDICO')")
    public AtendimentoDto iniciar(@PathVariable Long id) {
        return service.iniciar(id);
    }

    @PostMapping("/{id}/indicar-internacao")
    @PreAuthorize("hasRole('MEDICO')")
    public AtendimentoDto indicarInternacao(@PathVariable Long id, @Valid @RequestBody IndicarInternacaoRequest req) {
        return service.indicarInternacao(id, req.setorId());
    }

    /** Alta do pronto-socorro sem internação. */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    public AtendimentoDto finalizar(@PathVariable Long id) {
        return service.finalizar(id);
    }
}
