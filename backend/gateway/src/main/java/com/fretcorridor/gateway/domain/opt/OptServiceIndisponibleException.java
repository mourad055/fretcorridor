package com.fretcorridor.gateway.domain.opt;

public class OptServiceIndisponibleException extends RuntimeException {
    public OptServiceIndisponibleException() {
        super("service-bur (vue Bureau) indisponible");
    }
}
