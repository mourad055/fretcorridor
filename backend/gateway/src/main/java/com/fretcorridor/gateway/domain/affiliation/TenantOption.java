package com.fretcorridor.gateway.domain.affiliation;

/** S18 : un tenant sous lequel l'acteur connecté peut opérer. */
public record TenantOption(String tenantId, boolean origine) {
}
