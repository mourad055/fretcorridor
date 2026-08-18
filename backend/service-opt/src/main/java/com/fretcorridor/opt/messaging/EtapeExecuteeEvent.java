package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement EtapeExecutee (service-exe, Mobile -> OPT).
 * BROUILLON - contrat non encore valide avec Personne 1, cf
 * shared-contracts/asyncapi/events/etape-executee.yaml.
 *
 * Ferme EF-MAT-09 (figer l'exécuté) et conditionne EF-MAT-08/RG-058
 * (retour a vide non confirme avant livraison effective de l'aller).
 */
public record EtapeExecuteeEvent(
        UUID eventId,
        UUID missionId,
        TypeEtape typeEtape,
        Instant horodatageExecution
) {
    public enum TypeEtape { ENLEVEMENT, LIVRAISON }
}
