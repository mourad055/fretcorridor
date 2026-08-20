package com.fretcorridor.opt.messaging;

import java.util.List;
import java.util.UUID;

/** Miroir exact de DemandePublieeLotsPayload - contrat valide avec Personne 1 (Mobile). */
public record DemandePublieeLotsEvent(
        UUID eventId,
        UUID demandeId,
        List<LotPayload> lots
) {
}
