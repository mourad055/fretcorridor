package com.fretcorridor.gateway.domain.cap;

public class CapServiceIndisponibleException extends RuntimeException {
    public CapServiceIndisponibleException() {
        super("service-cap indisponible");
    }
}
