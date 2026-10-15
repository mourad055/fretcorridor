package com.flysoft.fretcorridor.cap.messaging;

import java.util.UUID;

/**
 * Contrat shared-contracts/asyncapi/events/demande-refusee-par-chauffeur.yaml
 * -- chauffeur refuse explicitement une proposition diffusee. transporteurId
 * OBLIGATOIRE (exclusion du chauffeur du prochain cycle sur cette demande,
 * cote OPT : DemandeEnAttente.transporteursExclus).
 */
public record DemandeRefuseeParChauffeurEvent(
        UUID eventId,
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId
) {
}
