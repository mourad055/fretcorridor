package com.fretcorridor.opt.web.dto;

import com.fretcorridor.opt.domain.Affectation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de sortie pour TRK - expose uniquement ce dont TRK a besoin pour
 * calculer un ETA (origine/destination/itineraire), plus les identifiants
 * de tracabilite. La tarification est incluse pour completude mais n'est
 * pas le besoin premier de TRK (utile a d'autres consommateurs futurs).
 */
public record AffectationResponse(
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        double origineLatitude,
        double origineLongitude,
        double destinationLatitude,
        double destinationLongitude,
        Double distanceMetres,
        Double dureeSecondes,
        Double intervalleConfianceSecondes,
        BigDecimal prixTransport,
        boolean tarificationModeDegrade,
        Instant dateCreation
) {
    public static AffectationResponse from(Affectation affectation) {
        return new AffectationResponse(
                affectation.getId(),
                affectation.getDemandeId(),
                affectation.getCapaciteId(),
                affectation.getOrigineLatitude(),
                affectation.getOrigineLongitude(),
                affectation.getDestinationLatitude(),
                affectation.getDestinationLongitude(),
                affectation.getDistanceMetres(),
                affectation.getDureeSecondes(),
                affectation.getIntervalleConfianceSecondes(),
                affectation.getPrixTransport(),
                affectation.isTarificationModeDegrade(),
                affectation.getDateCreation()
        );
    }
}
