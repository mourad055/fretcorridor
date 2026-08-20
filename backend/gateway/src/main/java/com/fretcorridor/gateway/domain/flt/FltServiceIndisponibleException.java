package com.fretcorridor.gateway.domain.flt;

public class FltServiceIndisponibleException extends RuntimeException {
    public FltServiceIndisponibleException() {
        super("service-flt indisponible");
    }
}
