package com.flysoft.fretcorridor.exe.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (shared-contracts/asyncapi/events/tournee-constituee.yaml,
 * BROUILLON au 2026-08-19 — à revalider au point de synchronisation
 * hebdomadaire des contrats d'API avant toute évolution de ce fichier).
 */
public record EtapeConstitueeDto(
        UUID missionId,
        int rang,
        String typeEtape,
        UUID demandeId,
        double pointLatitude,
        double pointLongitude,
        Instant fenetreDebut,
        Instant fenetreFin
) {
}
