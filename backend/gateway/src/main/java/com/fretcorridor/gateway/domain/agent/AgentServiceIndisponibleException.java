package com.fretcorridor.gateway.domain.agent;

public class AgentServiceIndisponibleException extends RuntimeException {
    public AgentServiceIndisponibleException() {
        super("service-ida indisponible");
    }
}
