package com.fretcorridor.gateway.domain.agent;

public class EnrolementIntrouvableException extends RuntimeException {
    public EnrolementIntrouvableException() {
        super("Enrôlement introuvable");
    }
}
