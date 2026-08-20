package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EF-CAP-03 (Sprint 4) : les trois modes de déclaration partagent ce même
 * type — volumeM3/longueurPlancherM optionnels selon le mode. Miroir du
 * contrat réel service-cap (CapaciteCreationRequest).
 */
public record DeclarationCapacite(
        String vehiculeId,
        String axeId,
        String modeDeclaration,
        BigDecimal poidsKg,
        BigDecimal volumeM3,
        BigDecimal longueurPlancherM,
        double origineLatitude,
        double origineLongitude,
        String typeVehicule,
        Double profilHauteurMetres,
        Double profilLargeurMetres,
        Double profilLongueurMetres,
        Double profilPoidsMaxTonnes,
        Double profilChargeMaxParEssieuTonnes,
        Integer profilNombreEssieux,
        boolean profilMatieresDangereuses,
        Instant dateDepart
) {
}
