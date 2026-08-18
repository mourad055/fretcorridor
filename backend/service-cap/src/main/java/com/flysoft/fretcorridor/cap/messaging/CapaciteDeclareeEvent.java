package com.flysoft.fretcorridor.cap.messaging;

import java.math.BigDecimal;
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
 *
 * capaciteResiduelleKg/volumeResiduelM3 : ajoutes pour fermer le bug remonte
 * le 18 aout (violation NOT NULL cote service-opt, capacite perdue
 * silencieusement) - service-opt les attend depuis EF-CAP-07 (sequencement
 * L2, Phase 2) mais service-cap ne les publiait jamais. Valeurs prises sur
 * Capacite.getCapaciteResiduelleKg()/getVolumeM3() (meme grandeur, nom
 * different pour volumeM3 cote service-cap).
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
        String typeVehicule,
        BigDecimal capaciteResiduelleKg,
        BigDecimal volumeResiduelM3
) {
}
