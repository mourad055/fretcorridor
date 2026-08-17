package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * EF-BUR-03 : indicateurs de marché pour un axe — volumes, prix observés
 * (médiane et dispersion), déséquilibre directionnel. RG-085 (EF-BUR-04) :
 * aucun indicateur n'est exposé si l'effectif sous-jacent est inférieur au
 * seuil d'agrégation configuré — tous masqués ensemble, pas un par un, car
 * ils partagent le même effectif (les missions de l'axe).
 *
 * Indicateurs du CDC (UC-BUR-02) volontairement absents faute de données
 * disponibles dans le modèle actuel (pas un oubli, cf. commit) : délais de
 * parcours et variabilité, taux de retour à vide, taux d'appariement et
 * délai moyen jusqu'à appariement, saisonnalité, segmentation par nature de
 * marchandise ou type de véhicule.
 */
public record ObservatoireAxe(
        UUID axeId,
        long seuil,
        Optional<Long> nombreMissions,
        Optional<BigDecimal> prixMediane,
        Optional<BigDecimal> prixDispersion,
        Optional<String> devise,
        Optional<Double> tauxDesequilibreDirectionnel
) {
    public static ObservatoireAxe sousLeSeuil(UUID axeId, long seuil) {
        return new ObservatoireAxe(axeId, seuil, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public static ObservatoireAxe calcule(UUID axeId, long seuil, long nombreMissions, BigDecimal prixMediane,
                                           BigDecimal prixDispersion, String devise, double tauxDesequilibreDirectionnel) {
        return new ObservatoireAxe(axeId, seuil, Optional.of(nombreMissions), Optional.of(prixMediane),
                Optional.of(prixDispersion), Optional.of(devise), Optional.of(tauxDesequilibreDirectionnel));
    }

    public boolean seuilAtteint() {
        return nombreMissions.isPresent();
    }
}
