package com.fretcorridor.opt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Moteur d'optimisation FretCorridor - perimetre Personne 3 (Moteur).
 *
 * Phase 1 (Sprint 5) : L0 filtrage H3 (appel synchrone interne a service-geo,
 * meme porteur, budget latence ~50ms) + L1 affectation Kuhn-Munkres.
 * Aucune interface directe - consomme en API/evenements par Mobile
 * (service-mkt recoit PropositionEmise, service-exe recoit AffectationConfirmee).
 */
@SpringBootApplication
@EnableScheduling // MatchingCycleService : cycle de matching par fenetre, EF-MAT-01
public class ServiceOptApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceOptApplication.class, args);
    }
}
