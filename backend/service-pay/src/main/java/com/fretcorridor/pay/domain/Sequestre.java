package com.fretcorridor.pay.domain;

import java.time.Instant;

/** État logique reflétant le cantonnement chez le prestataire (CDC §13) — 1-1 Mission. */
public record Sequestre(String missionId, SequestreEtat etat, Instant declencheLe, Instant libereLe) {
}
