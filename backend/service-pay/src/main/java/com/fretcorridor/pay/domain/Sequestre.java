package com.fretcorridor.pay.domain;

import java.time.Instant;

/**
 * État logique reflétant le cantonnement chez le prestataire (CDC §13) —
 * 1-1 Mission. {@code tenantId}/{@code transporteurId} sont {@code null}
 * tant que {@code DECLENCHE} — connus seulement à la libération (clôture),
 * cf. ADR 0015 : {@code libereLe} sert de point de départ au délai de
 * contestation pour l'ordonnanceur de reversement automatique (EF-PAY-08).
 */
public record Sequestre(String missionId, SequestreEtat etat, Instant declencheLe, Instant libereLe,
                         String tenantId, String transporteurId) {
}
