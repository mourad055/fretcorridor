package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import com.fretcorridor.gateway.domain.adm.EntreeJournalAuditVue;

import java.time.Instant;

public record EntreeJournalAuditResponse(String id, String tenantId, String acteurId, String action, String ressource, Instant horodatage) {
    public static EntreeJournalAuditResponse from(EntreeJournalAuditVue entree) {
        return new EntreeJournalAuditResponse(entree.id(), entree.tenantId(), entree.acteurId(), entree.action(),
                entree.ressource(), entree.horodatage());
    }
}
