package com.fretcorridor.gateway.domain;

/** Levée quand service-ida refuse l'inscription (ex. téléphone déjà utilisé). */
public class RegistrationRefuseeException extends RuntimeException {
    public RegistrationRefuseeException(String message) {
        super(message);
    }
}
