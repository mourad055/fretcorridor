package com.fretcorridor.gateway.domain.ida;

/** service-ida injoignable (timeout, connexion refusée, 5xx) ou delegationToken absent. */
public class CompteAdminServiceIndisponibleException extends RuntimeException {
    public CompteAdminServiceIndisponibleException() {
        super("service-ida indisponible");
    }
}
