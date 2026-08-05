package com.fretcorridor.pay.domain;

/**
 * ENF-FIN-01 (bloquant) : FretCorridor n'est jamais dépositaire des fonds
 * (PRD §0). Cette énumération ne comporte volontairement AUCUNE variante
 * représentant un compte de trésorerie propriété de FretCorridor — les seuls
 * comptes modélisables sont ceux détenus par des tiers (transporteur,
 * chargeur, séquestre chez le prestataire agréé). Ajouter une valeur du type
 * "COMPTE_FRETCORRIDOR" ferait échouer EnfFin01Test par construction.
 */
public enum TypeCompte {
    COMPTE_TRANSPORTEUR,
    COMPTE_CHARGEUR,
    COMPTE_SEQUESTRE_PRESTATAIRE
}
