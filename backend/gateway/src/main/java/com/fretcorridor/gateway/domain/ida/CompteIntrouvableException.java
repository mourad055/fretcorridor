package com.fretcorridor.gateway.domain.ida;

/** Compte introuvable pour l'id/tenant demandé (404) — service-ida a répondu 404. */
public class CompteIntrouvableException extends RuntimeException {
    public CompteIntrouvableException() {
        super("Compte introuvable");
    }
}
