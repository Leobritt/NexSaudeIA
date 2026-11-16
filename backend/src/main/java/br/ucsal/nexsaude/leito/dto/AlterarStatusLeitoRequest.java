package br.ucsal.nexsaude.leito.dto;

import br.ucsal.nexsaude.leito.StatusLeito;
import jakarta.validation.constraints.NotNull;

/** Muda status manualmente (higienização concluída, interdição, reserva). OCUPADO só via internação. */
public record AlterarStatusLeitoRequest(@NotNull StatusLeito status) {}
