package com.fretcorridor.gateway.domain.trk;

public class TrkServiceIndisponibleException extends RuntimeException {
    public TrkServiceIndisponibleException() {
        super("service-bur (vue Bureau) indisponible");
    }
}
