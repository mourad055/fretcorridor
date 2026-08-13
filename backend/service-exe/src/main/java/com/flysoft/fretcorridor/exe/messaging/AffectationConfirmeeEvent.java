package com.flysoft.fretcorridor.exe.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (shared-contracts/asyncapi) — copie locale
 * volontaire, pas de bibliothèque Java partagée entre porteurs (architecture
 * microservices, même principe que service-bur/AffectationConfirmeeEvent).
 */
public record AffectationConfirmeeEvent(
        UUID eventId,
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        UUID vehiculeId,
        UUID transporteurId,
        UUID chargeurId,
        UUID axeId,
        double origineLatitude,
        double origineLongitude,
        String origineNom,
        double destinationLatitude,
        double destinationLongitude,
        String destinationNom,
        Double distanceEstimeeMetres,
        Long dureeEstimeeSecondes,
        Long intervalleConfianceSecondes,
        String geometrieEncodee,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        BigDecimal montantVerseTransporteur,
        String devise,
        String modeCollecte,
        String modeRemise,
        Instant horodatageConfirmation
) {
}
