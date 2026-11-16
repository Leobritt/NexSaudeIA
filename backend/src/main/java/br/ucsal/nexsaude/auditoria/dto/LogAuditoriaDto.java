package br.ucsal.nexsaude.auditoria.dto;

import br.ucsal.nexsaude.auditoria.LogAuditoria;

import java.time.OffsetDateTime;
import java.util.Map;

public record LogAuditoriaDto(Long id, OffsetDateTime dataHora, String usuario, String acao,
                              String recurso, String idRecurso, Map<String, Object> detalhes) {
    public static LogAuditoriaDto de(LogAuditoria l) {
        return new LogAuditoriaDto(l.getId(), l.getDataHora(), l.getUsuario(), l.getAcao(),
                l.getRecurso(), l.getIdRecurso(), l.getDetalhes());
    }
}
