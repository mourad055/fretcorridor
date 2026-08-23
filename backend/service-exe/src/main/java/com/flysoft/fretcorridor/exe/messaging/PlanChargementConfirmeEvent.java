package com.flysoft.fretcorridor.exe.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Miroir du contrat service-opt (com.fretcorridor.opt.messaging.PlanChargementConfirmeEvent,
 * EF-MAT-13, CDC S8.7). Publié uniquement pour une Tournée CONFIRMÉE par
 * l'oracle de chargement (jamais pour un rejet) - contrat déjà publié côté
 * service-opt (topic "plan-chargement-confirme") mais resté sans
 * consommateur jusqu'au 23 août (audit de suivi) : ce listener complète le
 * flux plutôt que de redéfinir le contrat.
 */
public record PlanChargementConfirmeEvent(
        UUID eventId,
        UUID tourneeId,
        List<EtatChargementDto> etats,
        Instant dateGeneration
) {
}
