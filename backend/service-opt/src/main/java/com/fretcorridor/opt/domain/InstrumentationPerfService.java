package com.fretcorridor.opt.domain;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Instrumentation de performance du pipeline de matching (CDC S8.10 :
 * "P50, P95, P99 par couche", EF-PERF). Mesure sans dependance externe
 * (pas de micrometer dans le pom opt) : fenetres glissantes bornees en
 * memoire, percentiles calcules a la demande.
 *
 * Couches mesurees, alignees sur le budget de latence du CDC S8.10 :
 *  - L0_FILTRAGE_MS   (budget CDC < 50 ms) : reduction de l'espace candidats
 *  - L1_AFFECTATION_MS : appel service-mat + resolution Kuhn-Munkres
 *  - CYCLE_AXE_MS      : cycle complet d'un axe (L0 + L1 + persistance)
 *  - LATENCE_AFFECTATION_S : bout-en-bout demande (date_reception -> affectation),
 *    la metrique "metier" qui seule interesse l'exploitant
 *
 * Fenetre glissante a taille fixe bornee (ECHANTILLONS_MAX) pour borner la
 * memoire ; au-dela, on evict le plus ancien. Un redemarrage reinitialise
 * les mesures : accepte, ce sont des indicateurs de regime, pas des donnees
 * regulatoires (EF-MAT-11 couvre la tracabilite decisionnelle, persistee elle).
 */
@Service
public class InstrumentationPerfService {

    static final int ECHANTILLONS_MAX = 500;

    private final Deque<Double> l0FiltrageMs = new ArrayDeque<>();
    private final Deque<Double> l1AffectationMs = new ArrayDeque<>();
    private final Deque<Double> cycleAxeMs = new ArrayDeque<>();
    private final Deque<Double> latenceAffectationS = new ArrayDeque<>();

    private final ReadWriteLock verrou = new ReentrantReadWriteLock();

    public void recordL0FiltrageMs(double ms) {
        enregistrer(l0FiltrageMs, ms);
    }

    public void recordL1AffectationMs(double ms) {
        enregistrer(l1AffectationMs, ms);
    }

    public void recordCycleAxeMs(double ms) {
        enregistrer(cycleAxeMs, ms);
    }

    public void recordLatenceAffectationSecondes(double secondes) {
        enregistrer(latenceAffectationS, secondes);
    }

    private void enregistrer(Deque<Double> echantillons, double valeur) {
        if (!Double.isFinite(valeur)) {
            return; // jamais de NaN/Infini dans les statistiques
        }
        verrou.writeLock().lock();
        try {
            if (echantillons.size() >= ECHANTILLONS_MAX) {
                echantillons.pollFirst();
            }
            echantillons.addLast(valeur);
        } finally {
            verrou.writeLock().unlock();
        }
    }

    /**
     * Photographie des 4 metriques : n, moyenne, p50, p95, p99. Percentiles
     * par interpolation lineaire entre rangs adjacents (convention standard,
     * deterministe et testable).
     */
    public Map<String, StatistiquesMetrique> snapshot() {
        Map<String, StatistiquesMetrique> snapshot = new LinkedHashMap<>();
        verrou.readLock().lock();
        try {
            snapshot.put("L0_FILTRAGE_MS", statistiques(l0FiltrageMs));
            snapshot.put("L1_AFFECTATION_MS", statistiques(l1AffectationMs));
            snapshot.put("CYCLE_AXE_MS", statistiques(cycleAxeMs));
            snapshot.put("LATENCE_AFFECTATION_S", statistiques(latenceAffectationS));
        } finally {
            verrou.readLock().unlock();
        }
        return snapshot;
    }

    private StatistiquesMetrique statistiques(Deque<Double> echantillons) {
        List<Double> triees = new ArrayList<>(echantillons);
        if (triees.isEmpty()) {
            return new StatistiquesMetrique(0, 0, 0, 0, 0);
        }
        triees.sort(Double::compare);
        int n = triees.size();
        double somme = 0;
        for (double v : triees) {
            somme += v;
        }
        return new StatistiquesMetrique(
                n,
                somme / n,
                percentile(triees, 0.50),
                percentile(triees, 0.95),
                percentile(triees, 0.99));
    }

    static double percentile(List<Double> valeursTriees, double quantile) {
        if (valeursTriees.isEmpty()) {
            return 0.0;
        }
        if (valeursTriees.size() == 1) {
            return valeursTriees.get(0);
        }
        // Interpolation lineaire : rang = q x (n-1), partie entiere + fraction.
        double rangReel = quantile * (valeursTriees.size() - 1);
        int rangBas = (int) Math.floor(rangReel);
        int rangHaut = Math.min(rangBas + 1, valeursTriees.size() - 1);
        double fraction = rangReel - rangBas;
        return valeursTriees.get(rangBas)
                + fraction * (valeursTriees.get(rangHaut) - valeursTriees.get(rangBas));
    }

    /** Statistiques immuables d'une metrique (n=0 => tout a zero). */
    public record StatistiquesMetrique(int n, double moyenne, double p50, double p95, double p99) {
    }
}
