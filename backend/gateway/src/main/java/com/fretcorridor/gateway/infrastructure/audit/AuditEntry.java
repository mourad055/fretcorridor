package com.fretcorridor.gateway.infrastructure.audit;

import java.time.Instant;

public record AuditEntry(String operateurId, String action, String ressource, Instant horodatage) {
}
