package com.fretcorridor.gateway.domain.affiliation;

public class AffiliationServiceIndisponibleException extends RuntimeException {
    public AffiliationServiceIndisponibleException() {
        super("Service d'identité indisponible");
    }
}
