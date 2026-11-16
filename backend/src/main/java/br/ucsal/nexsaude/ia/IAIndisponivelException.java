package br.ucsal.nexsaude.ia;

/** A IA não respondeu ou respondeu algo inutilizável. O fluxo segue manual. */
public class IAIndisponivelException extends RuntimeException {
    public IAIndisponivelException(String motivo) {
        super(motivo);
    }

    public IAIndisponivelException(String motivo, Throwable causa) {
        super(motivo, causa);
    }
}
