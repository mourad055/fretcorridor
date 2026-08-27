package com.fretcorridor.gateway.domain.opt;

public class PropositionMissionServiceIndisponibleException extends RuntimeException {
    public PropositionMissionServiceIndisponibleException() {
        super("service-opt indisponible");
    }
}
