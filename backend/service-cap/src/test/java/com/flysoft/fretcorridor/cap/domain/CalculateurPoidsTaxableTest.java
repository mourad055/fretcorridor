package com.flysoft.fretcorridor.cap.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RG-100 (3e terme LDM) et RG-101 (coefficients par axe, pas une seule
 * valeur globale) — même classe, deux bloquants distincts de l'audit CDC du
 * 19 août.
 */
class CalculateurPoidsTaxableTest {

    // Mêmes valeurs de référence que application.yml (fretcorridor.cap.coefficient-*).
    private final CalculateurPoidsTaxable calculateur = new CalculateurPoidsTaxable(333.0, 1850.0);

    @Test
    void utilise_les_coefficients_de_reference_quand_aucun_parametre_d_axe_n_est_fourni() {
        BigDecimal resultat = calculateur.calculer(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1), BigDecimal.valueOf(10), null);

        // max(1000, 1*333, 10*1850) = 18500
        assertThat(resultat).isEqualByComparingTo("18500.00");
    }

    @Test
    void utilise_les_coefficients_de_l_axe_quand_ils_sont_definis_rg_101() {
        Map<String, Object> parametresAxe = Map.of(
                "coefficientVolumetriqueKgParM3", 500.0,
                "coefficientPlancherKgParLdm", 2000.0);

        BigDecimal resultat = calculateur.calculer(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1), BigDecimal.valueOf(10), parametresAxe);

        // max(1000, 1*500, 10*2000) = 20000 -- different du resultat "reference"
        // (18500) : preuve que l'axe l'emporte bien sur la config globale.
        assertThat(resultat).isEqualByComparingTo("20000.00");
    }

    @Test
    void retombe_sur_la_reference_quand_une_seule_cle_est_absente_des_parametres_de_l_axe() {
        Map<String, Object> parametresAxeIncomplet = Map.of("coefficientVolumetriqueKgParM3", 500.0);

        BigDecimal resultat = calculateur.calculer(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1), BigDecimal.valueOf(10), parametresAxeIncomplet);

        // volumetrique de l'axe (500) mais plancher de reference (1850, cle absente)
        // max(1000, 1*500, 10*1850) = 18500
        assertThat(resultat).isEqualByComparingTo("18500.00");
    }

    @Test
    void formule_a_trois_termes_rg_100() {
        // Cas exact de l'audit CDC du 19 aout : poids=1000, volume=1, plancher=10
        // -> le systeme doit retourner 18500 (3e terme LDM), pas 1000.
        BigDecimal resultat = calculateur.calculer(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1), BigDecimal.valueOf(10), null);

        assertThat(resultat).isEqualByComparingTo("18500.00");
    }
}
