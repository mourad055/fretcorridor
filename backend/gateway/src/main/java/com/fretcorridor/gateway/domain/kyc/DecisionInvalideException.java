package com.fretcorridor.gateway.domain.kyc;

/** RG-097 (CDC) : une décision doit être motivée par un état cible valide, jamais rouvrir un dossier déjà tranché sur un état incohérent. */
public class DecisionInvalideException extends RuntimeException {

    public DecisionInvalideException(String message) {
        super(message);
    }
}
