package com.fretcorridor.opt.messaging;

import com.fretcorridor.dto.PointGeoDto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement DemandePubliee (service-mkt, Mobile -> OPT/MAT).
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile, service-mkt).
 */
public record DemandePublieeEvent(
        UUID eventId,
        UUID demandeId,
        UUID axeId,
        Map<String, Double> valeursCriteres,
        PointGeoDto origine,
        PointGeoDto destination,
        BigDecimal poidsTaxableKg
) {
}
