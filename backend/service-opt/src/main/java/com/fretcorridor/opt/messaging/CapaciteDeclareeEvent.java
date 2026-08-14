package com.fretcorridor.opt.messaging;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.ProfilCamionDto;

import java.util.Map;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement CapaciteDeclaree (service-cap, Mobile -> OPT/MAT).
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile, service-cap).
 *
 * transporteurId/vehiculeId : ajoutes pour resoudre le bug remonte par
 * Personne 1 (S7, "Mes missions" vide - AffectationConfirmeeEvent.transporteurId
 * toujours null). Nullable en attendant que service-cap les publie reellement -
 * cf shared-contracts/asyncapi/events/capacite-declaree.yaml (brouillon separe).
 * Un candidat sans ces champs degrade seulement AffectationConfirmeeEvent
 * (transporteurId/vehiculeId restent null dedans), ne bloque jamais le
 * matching lui-meme (ENF-DIS-04) - coherent avec position/profilCamion deja
 * nullable ci-dessous.
 */
public record CapaciteDeclareeEvent(
        UUID eventId,
        UUID capaciteId,
        UUID axeId,
        UUID transporteurId,
        UUID vehiculeId,
        Map<String, Double> valeursCriteres,
        PointGeoDto position,
        ProfilCamionDto profilCamion,
        String typeVehicule
) {
}
