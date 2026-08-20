package com.flysoft.fretcorridor.cap.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * EF-CAP-01/02, RG-100 (CDC §8.3.3) : poids taxable en transport routier =
 * max(POIDS_REEL, VOLUME × RHO_tenant, LDM × LAMBDA_tenant) — trois
 * grandeurs, pas deux. Le 3e terme (mètres de plancher occupés) est
 * spécifique au routier : une marchandise légère mais non gerbable (moteur
 * sur palette) occupe un plancher qu'aucun autre colis ne peut utiliser —
 * l'omettre est la source d'erreur tarifaire la plus fréquente en groupage
 * routier, systématiquement défavorable au transporteur (audit CDC du
 * 19 août : exemple chiffré, sous-facturation ×18,5 sur ce cas précis).
 * Coefficients jamais codés en dur (RG-101, anti-patron CDC §4.5 G4) : lus
 * depuis la configuration.
 */
@Component
public class CalculateurPoidsTaxable {

    private final BigDecimal rhoTenantKgParM3;
    private final BigDecimal lambdaTenantKgParLdm;

    public CalculateurPoidsTaxable(
            @Value("${fretcorridor.cap.coefficient-volumetrique-kg-par-m3}") double rhoTenant,
            @Value("${fretcorridor.cap.coefficient-plancher-kg-par-ldm}") double lambdaTenant) {
        this.rhoTenantKgParM3 = BigDecimal.valueOf(rhoTenant);
        this.lambdaTenantKgParLdm = BigDecimal.valueOf(lambdaTenant);
    }

    /**
     * @param poidsKg          poids reel declare (toujours connu, quel que soit le mode)
     * @param volumeM3         volume declare - nullable en mode TOTALE (declaration
     *                         grossiere sans detail volumetrique) ; dans ce cas, le
     *                         terme volumetrique est absent de la comparaison.
     * @param longueurPlancherM metres de plancher occupes (LDM) - nullable pour la
     *                         meme raison ; absent de la comparaison si non declare.
     */
    public BigDecimal calculer(BigDecimal poidsKg, BigDecimal volumeM3, BigDecimal longueurPlancherM) {
        BigDecimal poidsTaxable = poidsKg;
        if (volumeM3 != null) {
            poidsTaxable = poidsTaxable.max(volumeM3.multiply(rhoTenantKgParM3));
        }
        if (longueurPlancherM != null) {
            poidsTaxable = poidsTaxable.max(longueurPlancherM.multiply(lambdaTenantKgParLdm));
        }
        return poidsTaxable.setScale(2, RoundingMode.HALF_UP);
    }
}
