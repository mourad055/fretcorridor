package com.flysoft.fretcorridor.cap.client;

public class ServiceOptIndisponibleException extends RuntimeException {
    public ServiceOptIndisponibleException() {
        super("Service Moteur (OPT) momentanement indisponible.");
    }
}
