package com.fretcorridor.gateway.domain.exe;

/** EF-EXE-02/04 (Sprint 7) : une étape réellement soumise par le chauffeur. */
public record EtapeExecution(String type, String libelle, String horodatageCapture, String horodatageTransmission) {
}
