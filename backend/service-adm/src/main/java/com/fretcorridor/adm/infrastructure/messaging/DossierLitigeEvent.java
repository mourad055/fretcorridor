package com.fretcorridor.adm.infrastructure.messaging;

import java.time.Instant;

/**
 * Miroir exact du contrat consommé par service-pay (EF-PAY-08) —
 * {@code actif=false} signifie que le dossier de litige est {@code CLOS},
 * {@code true} pour tout autre statut (OUVERT/EN_COURS/ESCALADE).
 */
public record DossierLitigeEvent(
        String eventId,
        String dossierId,
        String tenantId,
        String missionId,
        boolean actif,
        Instant horodatage
) {
}
