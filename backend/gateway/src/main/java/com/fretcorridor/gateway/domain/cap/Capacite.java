package com.fretcorridor.gateway.domain.cap;

import java.time.Instant;

/**
 * Vue minimale d'une capacité telle qu'exposée à la vue Transporteur (Sprint 4,
 * lecture seule). Le modèle complet (poids taxable calculé, contraintes
 * d'acceptation) appartient à service-cap/service-mkt (Mobile, CDC UC-CAP-01).
 */
public record Capacite(
        String id,
        String transporteurId,
        String vehicule,
        String origine,
        String destination,
        Instant departLe,
        double poidsTaxableKg,
        ModeCollecte modeCollecte,
        CapaciteEtat etat
) {
}
