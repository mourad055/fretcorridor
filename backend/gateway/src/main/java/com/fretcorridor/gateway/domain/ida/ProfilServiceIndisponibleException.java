package com.fretcorridor.gateway.domain.ida;

/** service-ida injoignable (timeout, connexion refusée, 5xx) ou delegationToken absent. */
public class ProfilServiceIndisponibleException extends RuntimeException {
    public ProfilServiceIndisponibleException() {
        super("service-ida indisponible");
    }
}
