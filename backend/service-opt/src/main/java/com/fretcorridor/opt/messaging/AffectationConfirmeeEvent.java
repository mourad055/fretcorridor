package com.fretcorridor.opt.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        Instant horodatageConfirmation,
        // Infos marchandise (audit de suivi Mobile) : pour que Mission
        // (service-exe) -> app Chauffeur sache ce qu'elle transporte.
        String typeEmballageNom,
        Integer quantite,
        BigDecimal poidsTaxableKg,
        // Infos destinataire (audit de suivi Mobile), meme raisonnement que
        // typeEmballageNom/quantite ci-dessus.
        String destinataireNom,
        String destinataireTelephone,
        // demandeModeCollecte : DOMICILE/POINT_RELAIS declare par le
        // chargeur (Demande.modeCollecte) - a NE PAS confondre avec
        // modeCollecte/modeRemise ci-dessus (DEPOT/RETRAIT, placeholders
        // internes au calcul d'itineraire, sans lien avec la demande).
        String demandeModeCollecte,
        String typeDisponibilite,
        Double poidsTotalKg,
        Boolean grandeValeur
) {}
