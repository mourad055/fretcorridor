package com.fretcorridor.mat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module de matching FretCorridor - perimetre Personne 3 (Moteur).
 *
 * Sprint 5 : cout composite multi-critere (EF-MAT-04), ponderations
 * configurables et versionnees (jamais codees en dur, CDC S12.4), tracabilite
 * de chaque decision via CycleMatching (EF-MAT-11/12). Appele en synchrone
 * interne par service-opt (meme porteur, budget latence L0 ~50ms).
 *
 * Aucune interface directe - service backend pur, sans exposition Mobile/Web.
 */
@SpringBootApplication
public class ServiceMatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceMatApplication.class, args);
    }
}
