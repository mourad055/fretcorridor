package com.fretcorridor.gateway.domain.exe;

public class ExeServiceIndisponibleException extends RuntimeException {
    public ExeServiceIndisponibleException() {
        super("service-exe indisponible");
    }
}
