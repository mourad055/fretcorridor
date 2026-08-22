package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.InstrumentationPerfService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Photographie des indicateurs de performance du matching (CDC S8.10 :
 * P50/P95/P99 par couche, EF-PERF). Lecture seule, meme surface d'authentification
 * que les autres endpoints opt (SecurityConfig : /api/opt/** authentifie).
 *
 * Consommation prevue : observabilite manuelle pendant la demo + futur
 * rattachement a l'observatoire bur (Phase 2). Aucune ecriture : un snapshot
 * est un etat de regime courant, pas une donnee metier.
 */
@RestController
@RequestMapping("/api/opt/perf/matching")
public class InstrumentationPerfController {

    private final InstrumentationPerfService instrumentationPerfService;

    public InstrumentationPerfController(InstrumentationPerfService instrumentationPerfService) {
        this.instrumentationPerfService = instrumentationPerfService;
    }

    @GetMapping
    public Map<String, Object> snapshot() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        instrumentationPerfService.snapshot().forEach((metrique, stats) -> {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("n", stats.n());
            detail.put("moyenne", arrondi(stats.moyenne()));
            detail.put("p50", arrondi(stats.p50()));
            detail.put("p95", arrondi(stats.p95()));
            detail.put("p99", arrondi(stats.p99()));
            reponse.put(metrique, detail);
        });
        return reponse;
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}
