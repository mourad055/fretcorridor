package com.fretcorridor.opt.messaging;

import java.util.UUID;

/**
 * Diffusion-course : un chauffeur refuse explicitement une proposition qui
 * lui a ete diffusee. Distinct de l'expiration automatique (une autre
 * demande a ete confirmee ailleurs) - ici c'est un refus actif du candidat
 * lui-meme.
 *
 * transporteurId est ajoute (vs. la 1ere version BROUILLON qui ne portait
 * que affectationId/demandeId/capaciteId) : il permet a OPT de persister le
 * chauffeur dans la liste d'exclusion de la demande (DemandeEnAttente
 * .transporteursExclus) et d'ecarter ses capacites du prochain cycle de
 * matching sur CETTE demande - on ne re-diffuse jamais a un chauffeur qui
 * vient de refuser (plan de reorientation, partie Chauffeur point 2).
 * Contrat aligne sur la convention shared-contracts/asyncapi/events
 * (demande-refusee-par-chauffeur.yaml).
 */
public record DemandeRefuseeParChauffeurEvent(
        UUID eventId,
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId
) {
}
