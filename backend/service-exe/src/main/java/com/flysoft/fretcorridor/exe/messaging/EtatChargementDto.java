package com.flysoft.fretcorridor.exe.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (com.fretcorridor.opt.messaging.EtatChargementDto).
 * etapeTourneeId référence l'EtapeTournee INTERNE à service-opt (pas la même
 * entité, ni le même id, que EtapeTournee côté service-exe) - la
 * corrélation avec l'EtapeTournee locale se fait par (tourneeId, rang), la
 * seule clé partagée entre les deux modèles (cf javadoc PlanChargementEtape).
 */
public record EtatChargementDto(
        UUID etapeTourneeId,
        int rang,
        Map<String, Object> chargesParEssieu
) {
}
