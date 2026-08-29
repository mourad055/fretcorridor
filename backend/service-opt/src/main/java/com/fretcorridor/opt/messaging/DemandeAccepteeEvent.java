package com.fretcorridor.opt.messaging;

import java.util.UUID;

/**
 * Diffusion-course (plan de reorientation post-demo) : un chauffeur accepte
 * une des propositions PROPOSEE qui lui ont ete diffusees. Premier arrive
 * gagne - la resolution de la course se fait cote OPT via
 * AffectationRepository.confirmerSiProposee (atomique), jamais en supposant
 * que le premier evenement recu ici est forcement le premier envoye (ordre
 * Kafka non garanti entre partitions).
 *
 * Contrat aligne sur la convention shared-contracts/asyncapi/events
 * (demande-acceptee.yaml), y compris le transporteurId optionnel (nullable
 * cote Mobile tant que le flux complet n'est pas brane).
 */
public record DemandeAccepteeEvent(
        UUID eventId,
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId
) {
}
