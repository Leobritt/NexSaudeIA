package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.paciente.dto.PacienteResumoDto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    private String cns;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false)
    private String sexo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "medicamentos_uso_continuo", nullable = false)
    private List<String> medicamentosUsoContinuo = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private List<String> alergias = new ArrayList<>();

    protected Paciente() {}

    public Paciente(String nome, String cpf, String cns, LocalDate dataNascimento, String sexo,
                    List<String> medicamentosUsoContinuo, List<String> alergias) {
        this.nome = nome;
        this.cpf = cpf;
        this.cns = cns;
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.medicamentosUsoContinuo = medicamentosUsoContinuo == null ? new ArrayList<>() : new ArrayList<>(medicamentosUsoContinuo);
        this.alergias = alergias == null ? new ArrayList<>() : new ArrayList<>(alergias);
    }

    public int idade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public PacienteResumoDto resumo() {
        return new PacienteResumoDto(id, nome, cpf, dataNascimento);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getCns() { return cns; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getSexo() { return sexo; }
    public List<String> getMedicamentosUsoContinuo() { return List.copyOf(medicamentosUsoContinuo); }
    public List<String> getAlergias() { return List.copyOf(alergias); }
}
