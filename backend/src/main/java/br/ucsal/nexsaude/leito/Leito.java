package br.ucsal.nexsaude.leito;

import jakarta.persistence.*;

@Entity
@Table(name = "leito")
public class Leito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLeito status;

    /** Lock otimista: duas alocações simultâneas no mesmo leito não passam juntas. */
    @Version
    private long version;

    protected Leito() {}

    public Leito(String codigo, Setor setor, StatusLeito status) {
        this.codigo = codigo;
        this.setor = setor;
        this.status = status;
    }

    void mudarPara(StatusLeito novo) {
        TransicaoLeito.validar(status, novo);
        this.status = novo;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Setor getSetor() { return setor; }
    public StatusLeito getStatus() { return status; }
}
