package br.ucsal.nexsaude.auditoria;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/** Entrada da trilha de auditoria (RF-11). Imutável (RN-006). */
@Entity
@Immutable
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String acao;

    @Column(nullable = false)
    private String recurso;

    @Column(name = "id_recurso")
    private String idRecurso;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> detalhes;

    protected LogAuditoria() {}

    public LogAuditoria(String usuario, String acao, String recurso, String idRecurso, Map<String, Object> detalhes) {
        this.dataHora = OffsetDateTime.now();
        this.usuario = usuario;
        this.acao = acao;
        this.recurso = recurso;
        this.idRecurso = idRecurso;
        this.detalhes = detalhes;
    }

    public Long getId() { return id; }
    public OffsetDateTime getDataHora() { return dataHora; }
    public String getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getRecurso() { return recurso; }
    public String getIdRecurso() { return idRecurso; }
    public Map<String, Object> getDetalhes() { return detalhes; }
}
