package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EF-MAT-05/06 (Sprint 11, CDC S8.6). Publie par service-opt uniquement
 * quand une Tournee consolidee (LTL) est confirmee - jamais pour une
 * Affectation FTL simple (deja entierement decrite par son propre
 * AffectationConfirmeeEvent).
 *
 * Repond au besoin explicite de Personne 1 (S11, ecran tournee
 * multi-etapes Chauffeur) : ordre des etapes + regroupement de plusieurs
 * Missions deja creees sous un meme tourneeId.
 *
 * Contrat : shared-contracts/asyncapi/events/tournee-constituee.yaml
 */
public record TourneeConstitueeEvent(
        UUID eventId,
        UUID tourneeId,
        UUID capaciteId,
        UUID axeId,
        List<EtapeConstitueeDto> etapes,
        Instant dateGeneration
) {
}
