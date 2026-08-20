package com.fretcorridor.pay.domain;

/**
 * EF-PAY-06/07 : moyen de paiement choisi par le chargeur à l'instruction
 * d'encaissement (CDC §7.6, UC-PAY-01, étape 2). Toujours renseigné sur une
 * écriture d'ENCAISSEMENT, jamais sur un REVERSEMENT (le mode ne concerne
 * que le sens chargeur → prestataire).
 */
public enum ModePaiement {
    MONNAIE_ELECTRONIQUE,
    VIREMENT,
    TERME_CONTRACTUEL,
    ESPECES
}
