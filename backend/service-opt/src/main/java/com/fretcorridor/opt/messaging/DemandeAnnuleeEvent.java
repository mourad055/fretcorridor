package com.fretcorridor.opt.messaging;

import java.util.UUID;

/** Miroir du contrat service-mkt (Mobile) - meme principe que DemandePublieeEvent. */
public record DemandeAnnuleeEvent(UUID eventId, UUID demandeId) {
}
