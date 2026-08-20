package com.fretcorridor.gateway.domain.exe;

/**
 * S7 : vue résumée pour "mes missions" côté chauffeur/transporteur.
 * tourneeId (S11, nullable) : non-null quand la mission fait partie d'une
 * Tournée consolidée (LTL) — voir {@link MissionExecutionPort#tournee}.
 */
public record MissionExecution(String missionId, String statut, String origineNom, String destinationNom,
                                String dateCreation, String tourneeId) {
}
