package br.ucsal.nexsaude.prescricao;

import br.ucsal.nexsaude.auth.Usuario;
import br.ucsal.nexsaude.common.RegraNegocioException;
import br.ucsal.nexsaude.leito.Internacao;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Prescrição médica digital (RF-07). Regras (RN-004 + design 7):
 * editar volta para NAO_VERIFICADA; assinar exige verificação feita e, com interação grave
 * ou IA indisponível, justificativa; assinada não muda mais.
 */
@Entity
@Table(name = "prescricao")
public class Prescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "internacao_id")
    private Internacao internacao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Usuario medico;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPrescricao status = StatusPrescricao.RASCUNHO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao", nullable = false)
    private StatusVerificacao statusVerificacao = StatusVerificacao.NAO_VERIFICADA;

    @Column(name = "sugestao_id")
    private Long sugestaoId;

    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinada_por_id")
    private Usuario assinadaPor;

    @Column(name = "assinada_em")
    private OffsetDateTime assinadaEm;

    @Column(name = "hash_assinatura", columnDefinition = "bpchar(64)")
    private String hashAssinatura;

    @ElementCollection
    @CollectionTable(name = "item_prescricao", joinColumns = @JoinColumn(name = "prescricao_id"))
    @OrderColumn(name = "ordem")
    private List<ItemPrescricao> itens = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "alerta_interacao", joinColumns = @JoinColumn(name = "prescricao_id"))
    @OrderColumn(name = "ordem")
    private List<AlertaInteracao> alertas = new ArrayList<>();

    protected Prescricao() {}

    public Prescricao(Internacao internacao, Usuario medico, List<ItemPrescricao> itens) {
        this.internacao = internacao;
        this.medico = medico;
        this.itens.addAll(itens);
    }

    void trocarItens(List<ItemPrescricao> novos) {
        exigirRascunho();
        itens.clear();
        itens.addAll(novos);
        alertas.clear();
        statusVerificacao = StatusVerificacao.NAO_VERIFICADA;
        sugestaoId = null;
    }

    void registrarVerificacao(StatusVerificacao resultado, List<AlertaInteracao> novos, Long sugestaoId) {
        exigirRascunho();
        alertas.clear();
        alertas.addAll(novos);
        statusVerificacao = resultado;
        this.sugestaoId = sugestaoId;
    }

    boolean temInteracaoGrave() {
        return alertas.stream().anyMatch(a -> a.getGravidade() == GravidadeInteracao.GRAVE);
    }

    boolean exigeJustificativa() {
        return temInteracaoGrave() || statusVerificacao == StatusVerificacao.IA_INDISPONIVEL;
    }

    void assinar(Usuario medico, String justificativa, OffsetDateTime quando, String hash) {
        exigirRascunho();
        if (statusVerificacao == StatusVerificacao.NAO_VERIFICADA) {
            throw new RegraNegocioException("Verifique as interações medicamentosas antes de assinar");
        }
        if (exigeJustificativa() && (justificativa == null || justificativa.isBlank())) {
            throw new RegraNegocioException(temInteracaoGrave()
                    ? "Interação grave: registre a justificativa clínica para assinar"
                    : "Verificação por IA indisponível: registre a justificativa para assinar");
        }
        this.justificativa = justificativa == null || justificativa.isBlank() ? null : justificativa.trim();
        this.assinadaPor = medico;
        this.assinadaEm = quando;
        this.hashAssinatura = hash;
        this.status = StatusPrescricao.ASSINADA;
    }

    private void exigirRascunho() {
        if (status != StatusPrescricao.RASCUNHO) {
            throw new RegraNegocioException("Prescrição assinada não pode ser alterada");
        }
    }

    public Long getId() { return id; }
    public Internacao getInternacao() { return internacao; }
    public Usuario getMedico() { return medico; }
    public StatusPrescricao getStatus() { return status; }
    public StatusVerificacao getStatusVerificacao() { return statusVerificacao; }
    public String getJustificativa() { return justificativa; }
    public OffsetDateTime getAssinadaEm() { return assinadaEm; }
    public String getHashAssinatura() { return hashAssinatura; }
    public List<ItemPrescricao> getItens() { return List.copyOf(itens); }
    public List<AlertaInteracao> getAlertas() { return List.copyOf(alertas); }
}
