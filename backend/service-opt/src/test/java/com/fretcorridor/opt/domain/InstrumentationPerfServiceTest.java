package com.fretcorridor.opt.domain;

import org.junit.jupiter.api.Test;

import java.util.List;




import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de l'instrumentation P50/P95/P99 (EF-PERF, CDC S8.10) : percentiles
 * par interpolation lineaire, fenetre glissante bornee, rejet des valeurs
 * non finies.
 */
class InstrumentationPerfServiceTest {

    // ---- percentile : interpolation lineaire -------------------------------

    @Test
    void percentile_seriesConnues_valeursExactes() {
        List<Double> serie = List.of(10.0, 20.0, 30.0, 40.0);
        assertEquals(25.0, InstrumentationPerfService.percentile(serie, 0.50),
                "p50 entre 20 et 30 (rang 1.5) = 25");
        assertEquals(38.5, InstrumentationPerfService.percentile(serie, 0.95),
                "p95 rang 2.85 = 30 + 0.85x10");
    }

    @Test
    void percentile_casDegeneres() {
        assertEquals(0.0, InstrumentationPerfService.percentile(List.of(), 0.95));
        assertEquals(42.0, InstrumentationPerfService.percentile(List.of(42.0), 0.95));
        assertEquals(10.0, InstrumentationPerfService.percentile(List.of(10.0, 20.0), 0.0));
    }

    // ---- snapshot : statistiques completes ---------------------------------

    @Test
    void snapshot_vide_toutAZero() {
        InstrumentationPerfService service = new InstrumentationPerfService();
        var stats = service.snapshot().get("L1_AFFECTATION_MS");
        assertNotNull(stats);
        assertEquals(0, stats.n());
        assertEquals(0.0, stats.p50());
        assertEquals(0.0, stats.p95());
    }

    @Test
    void snapshot_apresEnregistrements_p50EtP95Coherents() {
        InstrumentationPerfService service = new InstrumentationPerfService();
        for (double v = 1; v <= 100; v++) {
            service.recordL1AffectationMs(v);
        }
        var stats = service.snapshot().get("L1_AFFECTATION_MS");
        assertEquals(100, stats.n());
        assertEquals(50.5, stats.moyenne(), 0.001);
        assertEquals(50.5, stats.p50(), 0.001);
        assertTrue(stats.p95() >= 95.0 && stats.p95() <= 96.0,
                "p95 de 1..100 ~ 95.05, obtenu " + stats.p95());
    }

    // ---- fenetre glissante bornee ------------------------------------------

    @Test
    void auDelaDeLaTailleMax_lePlusAncienEstEvicte() {
        InstrumentationPerfService service = new InstrumentationPerfService();
        for (int i = 0; i < InstrumentationPerfService.ECHANTILLONS_MAX + 10; i++) {
            service.recordCycleAxeMs(i);
        }
        var stats = service.snapshot().get("CYCLE_AXE_MS");
        assertEquals(InstrumentationPerfService.ECHANTILLONS_MAX, stats.n(),
                "La memoire est bornee : jamais plus que ECHANTILLONS_MAX");
        // Echantillons restants : 10..509 (les 10 premiers ont ete evictes).
        // p99 = rang 0.99 x 499 = 494.01 -> valeur 504 + 0.01 x (505 - 504).
        assertEquals(504.01, stats.p99(), 0.001,
                "Les 10 premiers echantillons (0..9) ont bien ete evictes");
    }

    @Test
    void valeurNonFinie_jamaisEnregistree() {
        InstrumentationPerfService service = new InstrumentationPerfService();
        service.recordLatenceAffectationSecondes(Double.NaN);
        service.recordLatenceAffectationSecondes(Double.POSITIVE_INFINITY);
        assertEquals(0, service.snapshot().get("LATENCE_AFFECTATION_S").n());
    }

    @Test
    void metriquesIndependantes_parCouche() {
        InstrumentationPerfService service = new InstrumentationPerfService();
        service.recordL0FiltrageMs(12.0);
        service.recordL1AffectationMs(200.0);
        var l0 = service.snapshot().get("L0_FILTRAGE_MS");
        var l1 = service.snapshot().get("L1_AFFECTATION_MS");
        assertEquals(1, l0.n());
        assertEquals(12.0, l0.p50());
        assertEquals(200.0, l1.p50());
        assertNotEquals(l0.p50(), l1.p50());
    }
}
