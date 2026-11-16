package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.auth.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Anotação de evolução no prontuário, escrita pelo médico. */
@Entity
@Table(name = "evolucao_clinica")
public class EvolucaoClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false)
    private String texto;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    protected EvolucaoClinica() {}

    public EvolucaoClinica(Long pacienteId, Usuario autor, String texto) {
        this.pacienteId = pacienteId;
        this.autor = autor;
        this.texto = texto.trim();
    }

    public Long getId() { return id; }
    public Usuario getAutor() { return autor; }
    public String getTexto() { return texto; }
    public OffsetDateTime getDataHora() { return dataHora; }
}
