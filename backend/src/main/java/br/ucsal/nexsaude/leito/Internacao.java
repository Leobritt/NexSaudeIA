package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.paciente.Paciente;
import br.ucsal.nexsaude.triagem.Atendimento;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "internacao")
public class Internacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "leito_id")
    private Leito leito;

    @Column(nullable = false)
    private OffsetDateTime entrada = OffsetDateTime.now();

    private OffsetDateTime saida;

    @Column(name = "motivo_alta")
    private String motivoAlta;

    protected Internacao() {}

    public Internacao(Atendimento atendimento, Leito leito) {
        this.atendimento = atendimento;
        this.paciente = atendimento.getPaciente();
        this.leito = leito;
    }

    public boolean isAtiva() { return saida == null; }

    void trocarLeito(Leito novo) { this.leito = novo; }

    void darAlta(String motivo) {
        this.saida = OffsetDateTime.now();
        this.motivoAlta = motivo;
    }

    public Long getId() { return id; }
    public Atendimento getAtendimento() { return atendimento; }
    public Paciente getPaciente() { return paciente; }
    public Leito getLeito() { return leito; }
    public OffsetDateTime getEntrada() { return entrada; }
    public OffsetDateTime getSaida() { return saida; }
    public String getMotivoAlta() { return motivoAlta; }
}
