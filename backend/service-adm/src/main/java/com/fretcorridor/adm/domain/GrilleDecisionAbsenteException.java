package com.fretcorridor.adm.domain;

/**
 * RG-096 (CDC) : « toute décision s'appuie sur une grille versionnée » —
 * un opérateur ne doit jamais avoir à trancher sans grille (E1, UC-ADM-01).
 */
public class GrilleDecisionAbsenteException extends RuntimeException {

    public GrilleDecisionAbsenteException(String tenantId) {
        super("Aucune grille de décision définie pour le tenant " + tenantId);
    }
}
