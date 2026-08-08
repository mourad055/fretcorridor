package com.fretcorridor.opt.messaging;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.ProfilCamionDto;

import java.util.Map;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement CapaciteDeclaree (service-cap, Mobile -> OPT/MAT).
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile, service-cap).
 *
 * position/profilCamion/typeVehicule : ajoutes pour alimenter Valhalla et la
 * Tarification L4 en aval du cycle de matching (cf CandidatCoutDto) - tous
 * nullable, un candidat sans ces donnees degrade seulement le calcul aval,
 * ne bloque jamais le matching lui-meme (ENF-DIS-04).
 */
public record CapaciteDeclareeEvent(
        UUID eventId,
        UUID capaciteId,
        UUID axeId,
        Map<String, Double> valeursCriteres,
        PointGeoDto position,
        ProfilCamionDto profilCamion,
        String typeVehicule
) {
}
