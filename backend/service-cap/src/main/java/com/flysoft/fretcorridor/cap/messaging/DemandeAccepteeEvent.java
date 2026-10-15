package com.flysoft.fretcorridor.cap.messaging;

import java.util.UUID;

/**
 * Contrat shared-contracts/asyncapi/events/demande-acceptee.yaml -- chauffeur
 * accepte une proposition diffusee (modele diffusion-course). transporteurId
 * nullable au contrat, toujours peuple ici (JWT authentifie).
 */
public record DemandeAccepteeEvent(
        UUID eventId,
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId
) {
}
