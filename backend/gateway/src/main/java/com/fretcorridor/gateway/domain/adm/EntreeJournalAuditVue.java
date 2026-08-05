package com.fretcorridor.gateway.domain.adm;

import java.time.Instant;

public record EntreeJournalAuditVue(String id, String tenantId, String acteurId, String action, String ressource, Instant horodatage) {
}
