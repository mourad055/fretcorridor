package com.fretcorridor.opt.web.dto;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.StatutAffectation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de sortie cote OPT - expose origine/destination/itineraire/tarification
 * d'une mission a ses consommateurs internes (TRK pour l'ETA) et, enrichi de
 * transporteurId/statut, a la liste "mes propositions en attente" du
 * chauffeur (GET /api/opt/affectations/proposees, consomme par service-cap).
 */
public record AffectationResponse(
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        UUID transporteurId,
        String statut,
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
                affectation.getTransporteurId(),
                affectation.getStatut().name(),
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
