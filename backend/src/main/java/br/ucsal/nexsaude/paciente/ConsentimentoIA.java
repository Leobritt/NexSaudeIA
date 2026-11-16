package br.ucsal.nexsaude.paciente;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Registro de consentimento (ou revogação) para uso de dados na IA (RN-002). */
@Entity
@Table(name = "consentimento_ia")
public class ConsentimentoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private boolean concedido;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Column(name = "registrado_por", nullable = false)
    private String registradoPor;

    protected ConsentimentoIA() {}

    public ConsentimentoIA(Long pacienteId, boolean concedido, String registradoPor) {
        this.pacienteId = pacienteId;
        this.concedido = concedido;
        this.registradoPor = registradoPor;
        this.dataHora = OffsetDateTime.now();
    }

    public boolean isConcedido() { return concedido; }
    public OffsetDateTime getDataHora() { return dataHora; }
}
