package com.flysoft.fretcorridor.cap.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * EF-CAP-01/02 : poids taxable via la "regle du maximum" - le plus grand
 * des deux entre le poids reel declare et un poids equivalent derive du
 * volume (un chargement volumineux mais leger occupe quand meme la place
 * d'un chargement lourd). Coefficient jamais code en dur (anti-patron CDC
 * S12.4) : lu depuis la configuration.
 */
@Component
public class CalculateurPoidsTaxable {

    private final BigDecimal coefficientVolumetriqueKgParM3;

    public CalculateurPoidsTaxable(
            @Value("${fretcorridor.cap.coefficient-volumetrique-kg-par-m3}") double coefficient) {
        this.coefficientVolumetriqueKgParM3 = BigDecimal.valueOf(coefficient);
    }

    /**
     * @param poidsKg  poids reel declare (toujours connu, quel que soit le mode)
     * @param volumeM3 volume declare - nullable en mode TOTALE (declaration
     *                 grossiere sans detail volumetrique) ; dans ce cas, le
     *                 poids reel fait foi seul, pas d'estimation inventee.
     */
    public BigDecimal calculer(BigDecimal poidsKg, BigDecimal volumeM3) {
        if (volumeM3 == null) {
            return poidsKg;
        }
        BigDecimal poidsVolumetrique = volumeM3.multiply(coefficientVolumetriqueKgParM3);
        return poidsKg.max(poidsVolumetrique).setScale(2, RoundingMode.HALF_UP);
    }
}
