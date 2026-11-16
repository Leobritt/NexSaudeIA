package br.ucsal.nexsaude.ia;

import java.util.List;

/** Porta para a IA de apoio à decisão. Implementações: {@link IASimulada} e {@link ClaudeIA}. */
public interface AssistenteClinicoIA {

    /** @throws IAIndisponivelException se a IA falhar ou devolver algo inválido */
    ResultadoTriagemIA sugerirTriagem(DadosTriagemIA dados);

    /** @throws IAIndisponivelException se a IA falhar ou devolver algo inválido */
    ResultadoInteracaoIA verificarInteracoes(List<String> prescritos, List<String> usoContinuo);

    /** "SIMULADA" ou "CLAUDE". */
    String nomeImplementacao();

    String modelo();
}
