package br.ucsal.nexsaude.leito;

import br.ucsal.nexsaude.common.RegraNegocioException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static br.ucsal.nexsaude.leito.StatusLeito.*;

/** Máquina de estados do leito. RN-003: de HIGIENIZACAO só se vai para LIVRE. */
public final class TransicaoLeito {

    private static final Map<StatusLeito, Set<StatusLeito>> PERMITIDAS = Map.of(
            LIVRE, EnumSet.of(OCUPADO, RESERVADO, INTERDITADO),
            RESERVADO, EnumSet.of(OCUPADO, LIVRE),
            OCUPADO, EnumSet.of(HIGIENIZACAO),
            HIGIENIZACAO, EnumSet.of(LIVRE),
            INTERDITADO, EnumSet.of(HIGIENIZACAO));

    private TransicaoLeito() {}

    public static void validar(StatusLeito de, StatusLeito para) {
        if (!PERMITIDAS.get(de).contains(para)) {
            throw new RegraNegocioException("Leito não pode passar de " + de + " para " + para);
        }
    }
}
