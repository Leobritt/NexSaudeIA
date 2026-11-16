package br.ucsal.nexsaude.common;

/** Recurso inexistente. Vira HTTP 404. */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado");
    }
}
