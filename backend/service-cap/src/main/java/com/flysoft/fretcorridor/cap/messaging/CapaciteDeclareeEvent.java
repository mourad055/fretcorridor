package com.flysoft.fretcorridor.cap.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir EXACT du contrat CapaciteDeclareeEvent cote service-opt (Moteur) -
 * mêmes noms de champs, meme structure. Verifie ligne a ligne contre
 * backend/service-opt/.../messaging/CapaciteDeclareeEvent.java.
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
