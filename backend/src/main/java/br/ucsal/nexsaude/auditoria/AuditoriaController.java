package br.ucsal.nexsaude.auditoria;

import br.ucsal.nexsaude.auditoria.dto.LogAuditoriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('AUDITOR')")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LogAuditoriaDto> listar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String recurso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate,
            @PageableDefault(size = 50, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(usuario, recurso, de, ate, pageable).map(LogAuditoriaDto::de);
    }
}
