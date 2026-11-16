package br.ucsal.nexsaude.ia;

import br.ucsal.nexsaude.triagem.CorRisco;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Registro de uma chamada à IA e do que foi decidido sobre ela. */
@Entity
@Table(name = "sugestao_ia")
public class SugestaoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSugestao tipo;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> entrada;

    @Column(name = "resposta_bruta")
    private String respostaBruta;

    @Enumerated(EnumType.STRING)
    private CorRisco cor;

    private Double confianca;

    private String justificativa;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<InteracaoIA> interacoes;

    @Column(name = "versao_prompt", nullable = false)
    private String versaoPrompt;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String implementacao;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(name = "motivo_falha")
    private String motivoFalha;

    @Column(name = "risco_iminente", nullable = false)
    private boolean riscoIminente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSugestao status = StatusSugestao.PENDENTE;

    protected SugestaoIA() {}

    SugestaoIA(TipoSugestao tipo, Long pacienteId, Map<String, Object> entrada, String versaoPrompt,
               String modelo, String implementacao, boolean riscoIminente) {
        this.tipo = tipo;
        this.pacienteId = pacienteId;
        this.entrada = entrada;
        this.versaoPrompt = versaoPrompt;
        this.modelo = modelo;
        this.implementacao = implementacao;
        this.riscoIminente = riscoIminente;
    }

    void registrarTriagem(ResultadoTriagemIA r, String bruta) {
        this.sucesso = true;
        this.cor = r.cor();
        this.confianca = r.confianca();
        this.justificativa = r.justificativa();
        this.respostaBruta = bruta;
    }

    void registrarInteracoes(ResultadoInteracaoIA r, String bruta) {
        this.sucesso = true;
        this.interacoes = r.interacoes();
        this.respostaBruta = bruta;
    }

    void registrarFalha(String motivo) {
        this.sucesso = false;
        this.motivoFalha = motivo;
    }

    void setStatus(StatusSugestao status) { this.status = status; }

    public Long getId() { return id; }
    public TipoSugestao getTipo() { return tipo; }
    public Long getPacienteId() { return pacienteId; }
    public OffsetDateTime getDataHora() { return dataHora; }
    public Map<String, Object> getEntrada() { return entrada; }
    public String getRespostaBruta() { return respostaBruta; }
    public CorRisco getCor() { return cor; }
    public Double getConfianca() { return confianca; }
    public String getJustificativa() { return justificativa; }
    public List<InteracaoIA> getInteracoes() { return interacoes == null ? List.of() : interacoes; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public String getModelo() { return modelo; }
    public String getImplementacao() { return implementacao; }
    public boolean isSucesso() { return sucesso; }
    public String getMotivoFalha() { return motivoFalha; }
    public boolean isRiscoIminente() { return riscoIminente; }
    public StatusSugestao getStatus() { return status; }
}
