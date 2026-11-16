package br.ucsal.nexsaude.prescricao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Embeddable
public class AlertaInteracao {

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private List<String> medicamentos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GravidadeInteracao gravidade;

    @Column(nullable = false)
    private String descricao;

    private String recomendacao;

    protected AlertaInteracao() {}

    public AlertaInteracao(List<String> medicamentos, GravidadeInteracao gravidade, String descricao, String recomendacao) {
        this.medicamentos = new ArrayList<>(medicamentos);
        this.gravidade = gravidade;
        this.descricao = descricao;
        this.recomendacao = recomendacao;
    }

    public List<String> getMedicamentos() { return List.copyOf(medicamentos); }
    public GravidadeInteracao getGravidade() { return gravidade; }
    public String getDescricao() { return descricao; }
    public String getRecomendacao() { return recomendacao; }
}
