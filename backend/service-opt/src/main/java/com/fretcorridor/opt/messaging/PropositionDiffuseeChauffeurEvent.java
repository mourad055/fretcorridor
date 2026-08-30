package com.fretcorridor.opt.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Diffusion-course (trouvaille Mobile) : notifie qu'une Affectation PROPOSEE
 * a ete diffusee a un transporteur precis - "une proposition vient de vous
 * etre adressee" cote app chauffeur. Comble l'absence de notification a la
 * diffusion (le precedent PropositionEmiseEvent est 100% cote client/chargeur,
 * consomme par service-mkt). Consomme par service-cap / service-not cote
 * Mobile.
 *
 * Publie uniquement pour les affectations reelement diffusables (tarif non
 * degrade), au meme moment que PropositionEmiseEvent - pas pour une
 * affectation purement tracee en base.
 */
public record PropositionDiffuseeChauffeurEvent(
        UUID eventId,
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId,
        UUID axeId,
        UUID vehiculeId,
        BigDecimal prixTransport,
        Double distanceEstimeeMetres,
        Long dureeEstimeeSecondes,
        Instant horodatageDiffusion
) {}
