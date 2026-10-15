package com.flysoft.fretcorridor.cap.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Miroir de AffectationResponse (service-opt, GET /api/opt/affectations/proposees)
 * -- champs ignorés silencieusement si absents cote reponse grace a Jackson
 * (FAIL_ON_UNKNOWN_PROPERTIES desactive par defaut sur RestClient).
 */
public record AffectationProposeeDto(
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId,
        String statut,
        double origineLatitude,
        double origineLongitude,
        double destinationLatitude,
        double destinationLongitude,
        String origineNom,
        String destinationNom,
        Double distanceMetres,
        Double dureeSecondes,
        Double intervalleConfianceSecondes,
        BigDecimal prixTransport,
        boolean tarificationModeDegrade,
        Instant expireA,
        Instant dateCreation
) {
}
