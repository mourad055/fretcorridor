package com.fretcorridor.gateway.domain.exe;

public class MissionIntrouvableException extends RuntimeException {
    public MissionIntrouvableException() {
        super("Mission introuvable");
    }
}
