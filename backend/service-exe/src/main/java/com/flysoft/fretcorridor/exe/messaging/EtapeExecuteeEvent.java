package com.flysoft.fretcorridor.exe.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (shared-contracts/asyncapi/events/etape-executee.yaml,
 * BROUILLON au 2026-08-20 — à revalider au point de synchronisation
 * hebdomadaire des contrats d'API). Ferme EF-MAT-09 (Sprint 12, "figer
 * l'exécuté") et conditionne EF-MAT-08/RG-058 (retour à vide) côté OPT.
 */
public record EtapeExecuteeEvent(
        UUID eventId,
        UUID missionId,
        TypeEtape typeEtape,
        Instant horodatageExecution
) {
    public enum TypeEtape { ENLEVEMENT, LIVRAISON }
}
