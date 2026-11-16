package br.ucsal.nexsaude.triagem;

import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.paciente.Paciente;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Passagem do paciente pelo pronto-socorro, da chegada à alta. */
@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(nullable = false)
    private OffsetDateTime chegada = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento status = StatusAtendimento.AGUARDANDO_TRIAGEM;

    @Enumerated(EnumType.STRING)
    @Column(name = "cor_atual")
    private CorRisco corAtual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_responsavel_id")
    private Usuario medicoResponsavel;

    @Column(name = "setor_indicado_id")
    private Long setorIndicadoId;

    @Column(name = "finalizado_em")
    private OffsetDateTime finalizadoEm;

    @Version
    private long version;

    protected Atendimento() {}

    public Atendimento(Paciente paciente) {
        this.paciente = paciente;
    }

    void setStatus(StatusAtendimento status) {
        this.status = status;
        if (status == StatusAtendimento.FINALIZADO) this.finalizadoEm = OffsetDateTime.now();
    }

    void setCorAtual(CorRisco cor) { this.corAtual = cor; }
    void setMedicoResponsavel(Usuario medico) { this.medicoResponsavel = medico; }
    void setSetorIndicadoId(Long setorId) { this.setorIndicadoId = setorId; }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public OffsetDateTime getChegada() { return chegada; }
    public StatusAtendimento getStatus() { return status; }
    public CorRisco getCorAtual() { return corAtual; }
    public Usuario getMedicoResponsavel() { return medicoResponsavel; }
    public Long getSetorIndicadoId() { return setorIndicadoId; }
    public OffsetDateTime getFinalizadoEm() { return finalizadoEm; }
}
