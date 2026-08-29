package com.fretcorridor.trk.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du payload Kafka de l'evenement EtapeExecutee (service-exe, Mobile ->
 * OPT, cf shared-contracts/asyncapi/events/etape-executee.yaml et
 * EtapeExecuteeEvent cote service-opt). Consomme aussi par TRK (point 6 du
 * plan de reorientation) pour basculer "position estimee du colis" vers
 * "position GPS temps reel du chauffeur" une fois l'enlevement confirme.
 *
 * Volontairement distinct de ce que stocke OPT : chaque service implemente
 * son propre miroir (pas de code partage entre modules, cf Plan d'execution
 * S4.1).
 */
public record EtapeExecuteeEvent(
        UUID eventId,
        UUID missionId,
        TypeEtape typeEtape,
        Instant horodatageExecution
) {
    public enum TypeEtape { ENLEVEMENT, LIVRAISON }
}
