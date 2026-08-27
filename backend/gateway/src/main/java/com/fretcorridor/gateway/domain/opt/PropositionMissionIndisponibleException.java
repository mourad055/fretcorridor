package com.fretcorridor.gateway.domain.opt;

/**
 * UC-MAT-02, E3 (acceptation concurrente) / E1 (expiration) : la proposition
 * n'est plus EN_ATTENTE au moment de la reponse (deja acceptee/refusee par
 * une autre requete, ou expiree). Message clair et non culpabilisant
 * (RG-051) -- jamais presente comme une erreur de l'utilisateur.
 */
public class PropositionMissionIndisponibleException extends RuntimeException {
    public PropositionMissionIndisponibleException(String message) {
        super(message);
    }
}
