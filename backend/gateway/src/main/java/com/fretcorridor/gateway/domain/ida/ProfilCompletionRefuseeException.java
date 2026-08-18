package com.fretcorridor.gateway.domain.ida;

/** service-ida a refusé la complétion (acteur introuvable, requête invalide) — jamais une erreur technique. */
public class ProfilCompletionRefuseeException extends RuntimeException {
    public ProfilCompletionRefuseeException(String message) {
        super(message);
    }
}
