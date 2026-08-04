package com.fretcorridor.bur.domain;

import java.util.Optional;

/**
 * EF-BUR-04 (anticipé dès le premier endpoint, PRD §5.2) : l'API ne restitue
 * jamais une statistique calculée sur un effectif inférieur au seuil d'agrégation
 * configuré — {@code nombreMissions} est absent plutôt que d'exposer une valeur
 * potentiellement identifiante.
 */
public record AgregatMissions(String axeId, Optional<Long> nombreMissions, long seuil) {

    public static AgregatMissions depuisComptage(String axeId, long comptageBrut, long seuil) {
        Optional<Long> valeurExposee = comptageBrut >= seuil ? Optional.of(comptageBrut) : Optional.empty();
        return new AgregatMissions(axeId, valeurExposee, seuil);
    }

    public boolean seuilAtteint() {
        return nombreMissions.isPresent();
    }
}
