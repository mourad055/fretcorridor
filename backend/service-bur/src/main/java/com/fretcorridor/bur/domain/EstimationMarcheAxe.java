package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * EF-BUR-05, RG-087 : estimation déclarative du volume mensuel réel du
 * marché d'un axe, saisie par un agent Bureau (enquête terrain, dires
 * d'experts). Aucune source automatisée fiable de "marché réel" n'existe
 * dans le système (§14.1 du CDC : l'API du bureau de fret étatique n'a pu
 * être vérifiée ; EF-INT-05 interdit de conditionner une fonction cœur à une
 * intégration étatique) — cf. docs/adr/0017-estimation-marche-declarative-par-axe.md.
 *
 * Une seule estimation active par axe : redéfinir remplace la précédente,
 * {@code definieLe}/{@code definieParActeurId} tracent la dernière mise à
 * jour pour la prudence méthodologique exigée par RG-087 (l'analyste doit
 * pouvoir juger de la fraîcheur de l'estimation affichée à côté d'un
 * indicateur).
 */
public record EstimationMarcheAxe(
        String tenantId,
        UUID axeId,
        BigDecimal volumeMensuelEstime,
        String source,
        String definieParActeurId,
        Instant definieLe
) {
    public EstimationMarcheAxe {
        if (volumeMensuelEstime == null || volumeMensuelEstime.signum() <= 0) {
            throw new IllegalArgumentException("Le volume mensuel estimé doit être strictement positif");
        }
    }
}
