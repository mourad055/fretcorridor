package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.EntreeJournalAudit;

import java.time.Instant;

public record EntreeJournalAuditResponse(
        String id,
        String tenantId,
        String acteurId,
        String action,
        String ressource,
        Instant horodatage
) {
    public static EntreeJournalAuditResponse from(EntreeJournalAudit entree) {
        return new EntreeJournalAuditResponse(entree.id(), entree.tenantId(), entree.acteurId(), entree.action(),
                entree.ressource(), entree.horodatage());
    }
}
