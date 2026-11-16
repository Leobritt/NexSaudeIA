package br.ucsal.nexsaude.auth;

/** Perfis de acesso (ERS 1.3). O perfil PACIENTE fica fora do MVP. */
public enum Perfil {
    ADMIN, ENFERMEIRO, MEDICO, GESTOR_LEITOS, AUDITOR
}
