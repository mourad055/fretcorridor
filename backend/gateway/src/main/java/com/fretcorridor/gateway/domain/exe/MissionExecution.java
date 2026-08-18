package com.fretcorridor.gateway.domain.exe;

/** S7 : vue résumée pour "mes missions" côté chauffeur/transporteur. */
public record MissionExecution(String missionId, String statut, String origineNom, String destinationNom, String dateCreation) {
}
