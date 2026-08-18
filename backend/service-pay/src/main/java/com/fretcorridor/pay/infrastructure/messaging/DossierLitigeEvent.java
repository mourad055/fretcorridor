package com.fretcorridor.pay.infrastructure.messaging;

import java.time.Instant;

/**
 * Miroir exact de l'événement publié par service-adm
 * (com.fretcorridor.adm.infrastructure.messaging.DossierLitigeEvent) —
 * {@code actif=false} signifie que le dossier de litige est clos.
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
