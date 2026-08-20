package com.fretcorridor.adm.domain;

import java.time.Instant;

/** ENF-SEC-02 : trace inviolable d'une action sensible. Append-only par construction. */
public record EntreeJournalAudit(
        String id,
        String tenantId,
        String acteurId,
        String action,
        String ressource,
        Instant horodatage
) {
}
