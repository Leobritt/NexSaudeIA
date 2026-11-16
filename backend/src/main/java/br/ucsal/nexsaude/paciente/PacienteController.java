package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.paciente.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacientes;
    private final ProntuarioService prontuarios;

    public PacienteController(PacienteService pacientes, ProntuarioService prontuarios) {
        this.pacientes = pacientes;
        this.prontuarios = prontuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ENFERMEIRO','MEDICO')")
    public PacienteDto criar(@Valid @RequestBody PacienteRequest req) {
        return pacientes.criar(req);
    }

    /** Busca por CPF (com ou sem máscara) ou por parte do nome. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ENFERMEIRO','MEDICO')")
    public List<PacienteDto> pesquisar(@RequestParam(required = false) String cpf,
                                       @RequestParam(required = false) String nome) {
        return pacientes.pesquisar(cpf, nome);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENFERMEIRO','MEDICO')")
    public PacienteDto detalhar(@PathVariable Long id) {
        return pacientes.detalhar(id);
    }

    @PutMapping("/{id}/consentimento-ia")
    @PreAuthorize("hasAnyRole('ENFERMEIRO','MEDICO')")
    public PacienteDto consentimento(@PathVariable Long id, @Valid @RequestBody ConsentimentoRequest req) {
        return pacientes.definirConsentimento(id, req.concedido());
    }

    /** Conteúdo filtrado pelo perfil de quem consulta (RN-005). */
    @GetMapping("/{id}/prontuario")
    @PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO','GESTOR_LEITOS','AUDITOR')")
    public ProntuarioDto prontuario(@PathVariable Long id) {
        return prontuarios.prontuario(id);
    }

    @PostMapping("/{id}/evolucoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDICO')")
    public EvolucaoDto evolucao(@PathVariable Long id, @Valid @RequestBody EvolucaoRequest req) {
        return prontuarios.registrarEvolucao(id, req.texto());
    }
}
