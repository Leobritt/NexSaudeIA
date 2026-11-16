package br.ucsal.nexsaude.paciente;

import br.ucsal.nexsaude.auditoria.AuditoriaService;
import br.ucsal.nexsaude.auth.Perfil;
import br.ucsal.nexsaude.auth.UsuarioLogado;
import br.ucsal.nexsaude.leito.InternacaoService;
import br.ucsal.nexsaude.paciente.dto.EvolucaoDto;
import br.ucsal.nexsaude.paciente.dto.PacienteDto;
import br.ucsal.nexsaude.paciente.dto.ProntuarioDto;
import br.ucsal.nexsaude.prescricao.PrescricaoService;
import br.ucsal.nexsaude.triagem.AtendimentoService;
import br.ucsal.nexsaude.triagem.TriagemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Prontuário consolidado (RF-04 reduzido) com acesso por perfil (RN-005, simplificada):
 * MEDICO e AUDITOR veem tudo; ENFERMEIRO vê dados, atendimentos e triagens;
 * GESTOR_LEITOS vê só a identificação e as internações.
 */
@Service
public class ProntuarioService {

    private final PacienteService pacientes;
    private final EvolucaoClinicaRepository evolucoes;
    private final AtendimentoService atendimentos;
    private final TriagemService triagens;
    private final InternacaoService internacoes;
    private final PrescricaoService prescricoes;
    private final AuditoriaService auditoria;

    public ProntuarioService(PacienteService pacientes, EvolucaoClinicaRepository evolucoes,
                             AtendimentoService atendimentos, TriagemService triagens, InternacaoService internacoes,
                             PrescricaoService prescricoes, AuditoriaService auditoria) {
        this.pacientes = pacientes;
        this.evolucoes = evolucoes;
        this.atendimentos = atendimentos;
        this.triagens = triagens;
        this.internacoes = internacoes;
        this.prescricoes = prescricoes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public ProntuarioDto prontuario(Long pacienteId) {
        Paciente p = pacientes.buscar(pacienteId);
        Perfil perfil = UsuarioLogado.atual().getPerfil();
        auditoria.registrar("LEITURA_PRONTUARIO", "Paciente", pacienteId, Map.of("perfil", perfil.name()));

        boolean completo = perfil == Perfil.MEDICO || perfil == Perfil.AUDITOR;
        boolean enfermagem = perfil == Perfil.ENFERMEIRO;
        boolean gestor = perfil == Perfil.GESTOR_LEITOS;

        PacienteDto dados = gestor
                ? new PacienteDto(p.getId(), p.getNome(), p.getCpf(), null, p.getDataNascimento(), null,
                        List.of(), List.of(), false)
                : pacientes.dto(p);
        return new ProntuarioDto(dados,
                completo || enfermagem ? atendimentos.doPaciente(pacienteId) : List.of(),
                completo || enfermagem ? triagens.doPaciente(pacienteId) : List.of(),
                completo || gestor ? internacoes.doPaciente(pacienteId) : List.of(),
                completo ? prescricoes.doPaciente(pacienteId) : List.of(),
                completo ? evolucoes.findByPacienteIdOrderByDataHoraDesc(pacienteId).stream().map(ProntuarioService::dto).toList()
                        : List.of());
    }

    @Transactional
    public EvolucaoDto registrarEvolucao(Long pacienteId, String texto) {
        pacientes.buscar(pacienteId);
        EvolucaoClinica e = evolucoes.save(new EvolucaoClinica(pacienteId, UsuarioLogado.atual(), texto));
        auditoria.registrar("EVOLUCAO", "Paciente", pacienteId, Map.of("evolucaoId", e.getId()));
        return dto(e);
    }

    private static EvolucaoDto dto(EvolucaoClinica e) {
        return new EvolucaoDto(e.getId(), e.getAutor().getNome(), e.getTexto(), e.getDataHora());
    }
}
