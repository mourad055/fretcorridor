package com.fretcorridor.gateway.domain;

/**
 * Levée quand service-ida ne répond pas dans le délai imparti — distincte
 * d'{@link InvalidCredentialsException} : un identifiant/PIN erroné n'est pas
 * la même situation qu'un service en panne, et l'utilisateur ne doit pas
 * confondre les deux.
 */
public class AuthenticationServiceUnavailableException extends RuntimeException {

    public AuthenticationServiceUnavailableException() {
        super("Le service d'authentification est momentanément indisponible");
    }
}
