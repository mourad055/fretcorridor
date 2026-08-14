package com.flysoft.fretcorridor.cap.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir EXACT du contrat CapaciteDeclareeEvent cote service-opt (Moteur) -
 * mêmes noms de champs, meme structure. Verifie ligne a ligne contre
 * backend/service-opt/.../messaging/CapaciteDeclareeEvent.java.
 *
 * transporteurId/vehiculeId : ajoutes pour fermer le bug S7 ("Mes missions"
 * vide cote chauffeur, AffectationConfirmeeEvent.transporteurId toujours
 * null cote OPT). transporteurId resolu via ServiceFltClient au moment de
 * la declaration (best-effort, nullable - ENF-DIS-04).
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
