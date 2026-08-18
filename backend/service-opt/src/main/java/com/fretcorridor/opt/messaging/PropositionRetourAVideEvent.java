package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * EF-MAT-08 / RG-058 (Sprint 12, CDC S8.6). Publie par OPT une fois une
 * mission de fret terminee - Tournee TERMINEE (cas consolide, LTL) ou
 * Affectation FTL simple dont la livraison vient d'etre executee (cas
 * majoritaire en Phase 1, jamais sequencee en Tournee, cf
 * SequencementDeclencheur : une seule affectation sur une capacite ne cree
 * pas de Tournee).
 *
 * tourneeId et affectationId sont MUTUELLEMENT EXCLUSIFS, jamais les deux
 * renseignes en meme temps, jamais les deux absents - meme principe que
 * AffectationResultat.itineraire (deux cas de null jamais confondus) :
 *   - tourneeId != null, affectationId == null : retour a vide sur une
 *     tournee consolidee (LTL, Sprint 11+)
 *   - affectationId != null, tourneeId == null : retour a vide sur une
 *     affectation FTL simple (majoritaire en Phase 1)
 * Un consommateur (Mobile, TRK) doit tester lequel des deux est present,
 * jamais supposer tourneeId toujours renseigne.
 *
 * Contrat : shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml
 */
public record PropositionRetourAVideEvent(
        UUID eventId,
        UUID tourneeId,
        UUID affectationId,
        UUID capaciteId,
        UUID axeId,
        double pointDepartLatitude,
        double pointDepartLongitude,
        Instant dateGeneration
) {
}
