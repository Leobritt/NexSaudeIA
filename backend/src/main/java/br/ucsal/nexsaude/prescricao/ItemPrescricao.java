package br.ucsal.nexsaude.prescricao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ItemPrescricao {

    @Column(nullable = false)
    private String medicamento;
    @Column(nullable = false)
    private String dose;
    @Column(nullable = false)
    private String via;
    @Column(nullable = false)
    private String frequencia;

    protected ItemPrescricao() {}

    public ItemPrescricao(String medicamento, String dose, String via, String frequencia) {
        this.medicamento = medicamento.trim();
        this.dose = dose.trim();
        this.via = via.trim();
        this.frequencia = frequencia.trim();
    }

    public String getMedicamento() { return medicamento; }
    public String getDose() { return dose; }
    public String getVia() { return via; }
    public String getFrequencia() { return frequencia; }
}
