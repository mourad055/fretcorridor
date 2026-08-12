package com.fretcorridor.gateway.domain.not;

public class NotServiceIndisponibleException extends RuntimeException {
    public NotServiceIndisponibleException() {
        super("service-not indisponible");
    }
}
