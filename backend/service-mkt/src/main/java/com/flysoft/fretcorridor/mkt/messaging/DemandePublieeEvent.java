package com.flysoft.fretcorridor.mkt.messaging;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Miroir exact du contrat shared-contracts/asyncapi/events/demande-publiee.yaml
 * (brouillon Moteur, a valider ensemble). Champs origine/destination a plat
 * plutot qu'un sous-objet imbrique : plus simple ici, le contrat YAML accepte
 * les deux representations tant que le JSON serialise correspond au record
 * DemandePublieeEvent cote service-opt (memes noms de champs).
 */
public record DemandePublieeEvent(
        UUID eventId,
        UUID demandeId,
        UUID axeId,
        Map<String, Double> valeursCriteres,
        PointGeo origine,
        PointGeo destination,
        BigDecimal poidsTaxableKg
) {
    public record PointGeo(double latitude, double longitude) {
    }
}
