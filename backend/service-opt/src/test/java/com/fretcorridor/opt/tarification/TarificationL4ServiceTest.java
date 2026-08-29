package com.fretcorridor.opt.tarification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Plan de reorientation post-demo, partie Client point 3.1 : "prix calcule
 * sur poids volumetrique + distance, accepte/refuse par le client".
 *
 * Verifie que TarificationL4Service facture bien sur DEUX grandeurs, comme
 * l'exige le CDC (RG-100/RG-101 + TarificationResultat PRIX = COUT_BASE +
 * COUT_UNITAIRE x POIDS_TAXABLE + SOMME(COUT_SERVICES)) :
 *   1. la DISTANCE : COUT_BASE = coutBaseParKm x distance (terme kilometrique).
 *   2. le POIDS_TAXABLE : COUT_VARIABLE = coutUnitairePoidsTaxable x
 *      poidsTaxableKg, ou poidsTaxableKg est DEJA le "poids volumetrique"
 *      (regle du maximum max(poids reel, volume x rho) appliquee au-dessus,
 *       cote service-mkt / service-cap RG-101) - jamais le seul poids reel.
 *
 * Le poids volumetrique n'est pas recense ici : il arrive deja plie dans
 * poidsTaxableKg (cf DemandeService service-mkt). Ce test verrouille le
 * comportement Moteur : un poids volumetrique dominant eleve bien le prix.
 */
class TarificationL4ServiceTest {

    @Mock private BaremeTarificationRepository baremeRepository;
    @Mock private ComposantCoutServiceRepository composantRepository;

    private TarificationL4Service service;
    private BaremeTarification bareme;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        service = new TarificationL4Service(baremeRepository, composantRepository);

        bareme = new BaremeTarification();
        set(bareme, "id", UUID.randomUUID());
        set(bareme, "version", 1);
        set(bareme, "actif", true);
        set(bareme, "regime", "POIDS_TAXABLE");
        set(bareme, "coutBaseParKm", new BigDecimal("650.0000"));        // 650 FCFA/km
        set(bareme, "coutUnitairePoidsTaxable", new BigDecimal("11.5000")); // 11,5 FCFA/kg
        set(bareme, "prixPlancherActif", false);
        // borne tension [0;0.2] : on la fixe a 0 dans les appels pour isoler
        // la formule, mais le bareme doit fournir des bornes coherentes.
        set(bareme, "tensionMinFraction", new BigDecimal("0.0000"));
        set(bareme, "tensionMaxFraction", new BigDecimal("0.2000"));
        set(bareme, "commissionTauxFraction", new BigDecimal("0.1000"));

        when(baremeRepository.findFirstByAxeIdIsNullAndTypeVehiculeIsNullAndActifTrue())
                .thenReturn(Optional.of(bareme));
        when(composantRepository.findByBaremeId(any())).thenReturn(List.of());
    }

    @Test
    void prix_facture_sur_poids_et_distance() {
        // 1000 km, poidsTaxable = 2000 kg (poids volumetrique inclus).
        TarificationResultat r = service.calculer(null, "FOURGON",
                new BigDecimal("2000.0000"), 1_000_000.0, BigDecimal.ZERO);

        assertFalse(r.modeDegrade());
        // COUT_BASE = 650 x 1000 km = 650 000
        assertEquals(0, r.coutBase().compareTo(new BigDecimal("650000.0000")));
        // COUT_VARIABLE = 11,5 x 2000 kg = 23 000
        assertEquals(0, r.coutVariablePoidsTaxable().compareTo(new BigDecimal("23000.0000")));
        // PRIX = 650000 + 23000 + 0 (tension 0, pas de services, pas de plancher)
        assertEquals(0, r.prixTransportAvantPlancher().compareTo(new BigDecimal("673000.0000")));
        assertEquals(0, r.prixTransport().compareTo(new BigDecimal("673000.00")));
    }

    @Test
    void poids_volumetrique_dominant_eleve_le_prix() {
        // Meme distance, poids volumetrique dominant (poidsTaxable 4000 vs 2000).
        TarificationResultat leger = service.calculer(null, "FOURGON",
                new BigDecimal("2000.0000"), 1_000_000.0, BigDecimal.ZERO);
        TarificationResultat volumineux = service.calculer(null, "FOURGON",
                new BigDecimal("4000.0000"), 1_000_000.0, BigDecimal.ZERO);

        // seules les parties variables diffèrent : + 11,5 x 2000 kg = + 23 000
        assertEquals(0, (volumineux.coutVariablePoidsTaxable()
                .subtract(leger.coutVariablePoidsTaxable()))
                .compareTo(new BigDecimal("23000.0000")));
        assertTrue(volumineux.prixTransport().compareTo(leger.prixTransport()) > 0,
                "Un poids volumetrique plus eleve doit tarifer plus cher");
    }

    @Test
    void distance_plus_longue_eleve_le_prix() {
        // Meme poidsTaxable, distance 2 fois plus longue : COUT_BASE double.
        TarificationResultat court = service.calculer(null, "FOURGON",
                new BigDecimal("2000.0000"), 500_000.0, BigDecimal.ZERO);
        TarificationResultat plusLong = service.calculer(null, "FOURGON",
                new BigDecimal("2000.0000"), 1_000_000.0, BigDecimal.ZERO);

        assertEquals(0, plusLong.coutBase().subtract(court.coutBase())
                .compareTo(new BigDecimal("325000.0000")));
        assertTrue(plusLong.prixTransport().compareTo(court.prixTransport()) > 0,
                "Une distance plus longue doit tarifer plus cher");
    }

    @Test
    void regime_poids_taxable_sans_distance_mode_degrades() {
        TarificationResultat r = service.calculer(null, "FOURGON",
                new BigDecimal("2000.0000"), null, BigDecimal.ZERO);
        assertTrue(r.modeDegrade(),
                "En regime POIDS_TAXABLE la distance est indispensable (COUT_BASE) - mode degrade plutot qu'un prix invente");
    }

    private static void set(Object cible, String champ, Object valeur) throws Exception {
        Field f = cible.getClass().getDeclaredField(champ);
        f.setAccessible(true);
        f.set(cible, valeur);
    }
}
