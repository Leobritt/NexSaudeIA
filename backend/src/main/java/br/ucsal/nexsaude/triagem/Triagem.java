package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.triagem.dto.SinaisVitais;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Uma classificação de risco. Reclassificar cria outra linha (RN-007). */
@Entity
@Table(name = "triagem")
public class Triagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(nullable = false)
    private String sintomas;

    @Column(name = "pa_sistolica") private Integer paSistolica;
    @Column(name = "pa_diastolica") private Integer paDiastolica;
    private Double temperatura;
    private Integer spo2;
    private Integer fc;
    private Integer fr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorRisco cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemCor origem;

    @Column(name = "sugestao_id")
    private Long sugestaoId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enfermeiro_id")
    private Usuario enfermeiro;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    protected Triagem() {}

    public Triagem(Atendimento atendimento, String sintomas, SinaisVitais s, CorRisco cor, OrigemCor origem,
                   Long sugestaoId, Usuario enfermeiro) {
        this.atendimento = atendimento;
        this.sintomas = sintomas;
        this.paSistolica = s.paSistolica();
        this.paDiastolica = s.paDiastolica();
        this.temperatura = s.temperatura();
        this.spo2 = s.spo2();
        this.fc = s.fc();
        this.fr = s.fr();
        this.cor = cor;
        this.origem = origem;
        this.sugestaoId = sugestaoId;
        this.enfermeiro = enfermeiro;
    }

    public SinaisVitais sinais() {
        return new SinaisVitais(paSistolica, paDiastolica, temperatura, spo2, fc, fr);
    }

    public Long getId() { return id; }
    public Atendimento getAtendimento() { return atendimento; }
    public String getSintomas() { return sintomas; }
    public CorRisco getCor() { return cor; }
    public OrigemCor getOrigem() { return origem; }
    public Long getSugestaoId() { return sugestaoId; }
    public Usuario getEnfermeiro() { return enfermeiro; }
    public OffsetDateTime getDataHora() { return dataHora; }
}
