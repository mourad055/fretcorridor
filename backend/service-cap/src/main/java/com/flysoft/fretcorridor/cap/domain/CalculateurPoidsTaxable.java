package com.flysoft.fretcorridor.cap.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * EF-CAP-01/02, RG-100 (CDC §8.3.3) : poids taxable en transport routier =
 * max(POIDS_REEL, VOLUME × RHO_tenant, LDM × LAMBDA_tenant) — trois
 * grandeurs, pas deux. Le 3e terme (mètres de plancher occupés) est
 * spécifique au routier : une marchandise légère mais non gerbable (moteur
 * sur palette) occupe un plancher qu'aucun autre colis ne peut utiliser —
 * l'omettre est la source d'erreur tarifaire la plus fréquente en groupage
 * routier, systématiquement défavorable au transporteur (audit CDC du
 * 19 août : exemple chiffré, sous-facturation ×18,5 sur ce cas précis).
 *
 * RG-101 (CDC §8.3.3, bloquant audit du 19 août) : RHO/LAMBDA ne doivent
 * jamais être une seule valeur globale pour tout le système -- versionnés
 * par tenant et par axe. Résolus depuis Axe.parametres (service-geo, mêmes
 * clés que celles écrites côté configuration d'axe, même mécanisme que
 * detourMaxDistanceKm/EF-MAT-10 côté service-opt) quand fournis ; repli sur
 * les valeurs de référence globales (fretcorridor.cap.coefficient-*)
 * sinon -- jamais un défaut inventé, seulement la valeur de référence déjà
 * validée par le métier avant ce correctif.
 */
@Component
public class CalculateurPoidsTaxable {

    private static final String CLE_RHO = "coefficientVolumetriqueKgParM3";
    private static final String CLE_LAMBDA = "coefficientPlancherKgParLdm";

    private final BigDecimal rhoReferenceKgParM3;
    private final BigDecimal lambdaReferenceKgParLdm;

    public CalculateurPoidsTaxable(
            @Value("${fretcorridor.cap.coefficient-volumetrique-kg-par-m3}") double rhoReference,
            @Value("${fretcorridor.cap.coefficient-plancher-kg-par-ldm}") double lambdaReference) {
        this.rhoReferenceKgParM3 = BigDecimal.valueOf(rhoReference);
        this.lambdaReferenceKgParLdm = BigDecimal.valueOf(lambdaReference);
    }

    /**
     * @param poidsKg          poids reel declare (toujours connu, quel que soit le mode)
     * @param volumeM3         volume declare - nullable en mode TOTALE (declaration
     *                         grossiere sans detail volumetrique) ; dans ce cas, le
     *                         terme volumetrique est absent de la comparaison.
     * @param longueurPlancherM metres de plancher occupes (LDM) - nullable pour la
     *                         meme raison ; absent de la comparaison si non declare.
     * @param parametresAxe    Axe.parametres (RG-101) - cles CLE_RHO/CLE_LAMBDA
     *                         lues si presentes, repli sur la reference globale sinon.
     */
    public BigDecimal calculer(BigDecimal poidsKg, BigDecimal volumeM3, BigDecimal longueurPlancherM,
                                Map<String, Object> parametresAxe) {
        BigDecimal rho = coefficientOuReference(parametresAxe, CLE_RHO, rhoReferenceKgParM3);
        BigDecimal lambda = coefficientOuReference(parametresAxe, CLE_LAMBDA, lambdaReferenceKgParLdm);

        BigDecimal poidsTaxable = poidsKg;
        if (volumeM3 != null) {
            poidsTaxable = poidsTaxable.max(volumeM3.multiply(rho));
        }
        if (longueurPlancherM != null) {
            poidsTaxable = poidsTaxable.max(longueurPlancherM.multiply(lambda));
        }
        return poidsTaxable.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal coefficientOuReference(Map<String, Object> parametresAxe, String cle, BigDecimal reference) {
        if (parametresAxe == null) {
            return reference;
        }
        Object valeur = parametresAxe.get(cle);
        return (valeur instanceof Number n) ? BigDecimal.valueOf(n.doubleValue()) : reference;
    }
}
