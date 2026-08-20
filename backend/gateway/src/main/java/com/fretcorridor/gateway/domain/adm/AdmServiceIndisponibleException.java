package com.fretcorridor.gateway.domain.adm;

public class AdmServiceIndisponibleException extends RuntimeException {
    public AdmServiceIndisponibleException() {
        super("service-adm indisponible");
    }
}
