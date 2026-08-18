package com.fretcorridor.gateway.domain.agent;

/** service-ida a refusé l'opération (téléphone déjà utilisé, OTP incorrect/expiré...). */
public class EnrolementRefuseException extends RuntimeException {
    public EnrolementRefuseException(String message) {
        super(message);
    }
}
