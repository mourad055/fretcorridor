package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du calcul des 7 termes du cout composite (CDC S8.5.3) - logique pure
 * extraite en methodes statiques package-private pour etre testable sans
 * contexte Spring (meme convention que MatchingCycleServiceTest).
 */
class CalculateurCoutSeptTermesTest {

    private final CalculateurCoutSeptTermes calculateur =
            new CalculateurCoutSeptTermes(50.0, 5.0);

    // ---- KM_APPROCHE -------------------------------------------------------

    @Test
    void kmApproche_memePoint_zero() {
        PointGeoDto douala = new PointGeoDto(4.0511, 9.7679);
        assertEquals(0.0, CalculateurCoutSeptTermes.kmApproche(douala, douala), 0.001);
    }

    @Test
    void kmApproche_donneeManquante_zeroNeutre_jamaisUnePenaliteInventee() {
        assertEquals(0.0, CalculateurCoutSeptTermes.kmApproche(null,
                new PointGeoDto(4.05, 9.77)));
        assertEquals(0.0, CalculateurCoutSeptTermes.kmApproche(
                new PointGeoDto(4.05, 9.77), null));
    }

    @Test
    void kmApproche_distanceReelle_positiveEtOrdreDeGrandeurKm() {
        // Douala -> Yaounde : environ 200-250 km a vol d'oiseau.
        double km = CalculateurCoutSeptTermes.kmApproche(
                new PointGeoDto(4.0511, 9.7679), new PointGeoDto(3.848, 11.502));
        assertTrue(km > 150 && km < 300, "Douala-Yaounde ~ 210 km, obtenu " + km);
    }

    // ---- ECART_TEMPOREL ----------------------------------------------------

    @Test
    void ecartTemporel_dansLaFenetre_ouFenetreNulle_zero() {
        Instant debut = Instant.parse("2026-08-22T08:00:00Z");
        Instant fin = Instant.parse("2026-08-22T12:00:00Z");
        assertEquals(0.0, CalculateurCoutSeptTermes.ecartTemporelHeures(
                debut, fin, Instant.parse("2026-08-22T10:00:00Z")));
        assertEquals(0.0, CalculateurCoutSeptTermes.ecartTemporelHeures(
                null, null, Instant.now()));
    }

    @Test
    void ecartTemporel_tropTot_heuresDAvance() {
        Instant debut = Instant.parse("2026-08-22T12:00:00Z");
        Instant fin = Instant.parse("2026-08-22T18:00:00Z");
        assertEquals(2.0, CalculateurCoutSeptTermes.ecartTemporelHeures(
                debut, fin, Instant.parse("2026-08-22T10:00:00Z")), 0.001);
    }

    @Test
    void ecartTemporel_tropTard_heuresDeRetard() {
        Instant debut = Instant.parse("2026-08-22T08:00:00Z");
        Instant fin = Instant.parse("2026-08-22T12:00:00Z");
        assertEquals(1.5, CalculateurCoutSeptTermes.ecartTemporelHeures(
                debut, fin, Instant.parse("2026-08-22T13:30:00Z")), 0.001);
    }

    // ---- GAIN_REMPLISSAGE --------------------------------------------------

    @Test
    void gainRemplissage_ratioBorneAUn() {
        assertEquals(0.5, CalculateurCoutSeptTermes.gainRemplissage(
                new BigDecimal("5000"), new BigDecimal("10000")), 0.001);
        assertEquals(1.0, CalculateurCoutSeptTermes.gainRemplissage(
                new BigDecimal("15000"), new BigDecimal("10000")),
                "Une demande plus lourde que le restant ne gagne pas plus qu'un remplissage complet");
    }

    @Test
    void gainRemplissage_donneesManquantesOuCapaciteEpuisee_zero() {
        assertEquals(0.0, CalculateurCoutSeptTermes.gainRemplissage(null, new BigDecimal("100")));
        assertEquals(0.0, CalculateurCoutSeptTermes.gainRemplissage(new BigDecimal("100"), null));
        assertEquals(0.0, CalculateurCoutSeptTermes.gainRemplissage(
                new BigDecimal("100"), BigDecimal.ZERO),
                "Capacite residuelle nulle ou negative : division interdite, terme neutre");
    }

    // ---- FIABILITE ---------------------------------------------------------

    @Test
    void fiabilite_valeurPublieeParCap_relaiTelQuel() {
        assertEquals(0.8, CalculateurCoutSeptTermes.fiabilite(Map.of("FIABILITE", 0.8)));
    }

    @Test
    void fiabilite_absente_placeholderNeutre_pasZeroBarrierePourNouvelEntrant() {
        assertEquals(CalculateurCoutSeptTermes.FIABILITE_DEFAUT_NEUTRE,
                CalculateurCoutSeptTermes.fiabilite(Map.of()));
        assertEquals(CalculateurCoutSeptTermes.FIABILITE_DEFAUT_NEUTRE,
                CalculateurCoutSeptTermes.fiabilite(null));
    }

    // ---- VALEUR_RETOUR -----------------------------------------------------

    @Test
    void valeurRetour_lineaireJusquaSaturation_puisPlafonne() {
        assertEquals(0.4, CalculateurCoutSeptTermes.valeurRetour(2, 5.0), 0.001);
        assertEquals(1.0, CalculateurCoutSeptTermes.valeurRetour(5, 5.0));
        assertEquals(1.0, CalculateurCoutSeptTermes.valeurRetour(50, 5.0),
                "Au-dela de la saturation, pas plus qu'un remplissage parfait");
        assertEquals(0.0, CalculateurCoutSeptTermes.valeurRetour(0, 5.0));
    }

    // ---- calculer() : la carte complete des 7 criteres CDC -----------------

    @Test
    void calculer_contientExactementLesSeptCriteresCDC() {
        DemandeEnAttente demande = new DemandeEnAttente(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Map.of(),
                new PointGeoDto(4.0511, 9.7679), new PointGeoDto(3.848, 11.502),
                new BigDecimal("5000"),
                Instant.parse("2026-08-22T08:00:00Z"), Instant.parse("2026-08-22T12:00:00Z"),
                "Palette", 10);
        CapaciteEnAttente capacite = new CapaciteEnAttente(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Map.of("FIABILITE", 0.7),
                new PointGeoDto(4.05, 9.77), null, "Semi-remorque",
                new BigDecimal("10000"), new BigDecimal("40"));
        Instant maintenant = Instant.parse("2026-08-22T09:00:00Z");

        Map<String, Double> criteres = calculateur.calculer(demande, capacite, maintenant, 3L, 0.5);

        assertEquals(java.util.Set.of("KM_APPROCHE", "KM_DETOUR", "ECART_TEMPOREL",
                        "GAIN_REMPLISSAGE", "FIABILITE", "RISQUE_AXE", "VALEUR_RETOUR"),
                criteres.keySet(), "Les 7 termes du CDC S8.5.3, sans autre cle");
        assertEquals(0.0, criteres.get("KM_DETOUR"),
                "Terme neutre au stade L1 (pas de tournee existante avant sequencement)");
        assertEquals(0.5, criteres.get("RISQUE_AXE"));
        assertEquals(0.7, criteres.get("FIABILITE"));
        assertEquals(0.5, criteres.get("GAIN_REMPLISSAGE"), 0.001);
        assertEquals(0.6, criteres.get("VALEUR_RETOUR"), 0.001,
                "3 demandes proches / saturation 5");
        assertEquals(0.0, criteres.get("ECART_TEMPOREL"), 0.001,
                "09h00 est dans la fenetre [08h, 12h]");
        assertTrue(criteres.get("KM_APPROCHE") >= 0.0);
    }
}
