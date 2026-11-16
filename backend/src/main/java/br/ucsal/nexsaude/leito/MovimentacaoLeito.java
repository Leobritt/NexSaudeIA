package br.ucsal.nexsaude.leito;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Histórico de transferências entre leitos. */
@Entity
@Table(name = "movimentacao_leito")
public class MovimentacaoLeito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internacao_id", nullable = false)
    private Long internacaoId;

    @Column(name = "leito_origem_id", nullable = false)
    private Long leitoOrigemId;

    @Column(name = "leito_destino_id", nullable = false)
    private Long leitoDestinoId;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    @Column(nullable = false)
    private String usuario;

    protected MovimentacaoLeito() {}

    public MovimentacaoLeito(Long internacaoId, Long leitoOrigemId, Long leitoDestinoId, String usuario) {
        this.internacaoId = internacaoId;
        this.leitoOrigemId = leitoOrigemId;
        this.leitoDestinoId = leitoDestinoId;
        this.usuario = usuario;
    }
}
