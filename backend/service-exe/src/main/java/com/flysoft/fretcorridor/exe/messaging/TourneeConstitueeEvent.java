package com.flysoft.fretcorridor.exe.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (shared-contracts/asyncapi/events/tournee-constituee.yaml,
 * BROUILLON au 2026-08-19). Publié par service-opt uniquement quand une
 * Tournée consolidée (LTL) est confirmée — jamais pour une Affectation FTL
 * simple, déjà entièrement décrite par son propre AffectationConfirmeeEvent.
 *
 * etapes[].missionId = même UUID qu'AffectationConfirmeeEvent.missionId :
 * clé de corrélation avec les Missions déjà créées ici par
 * AffectationConfirmeeListener.
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
