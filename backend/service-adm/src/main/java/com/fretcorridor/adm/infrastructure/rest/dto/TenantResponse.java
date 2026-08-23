package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.Tenant;

public record TenantResponse(String id, String nom, String pays, boolean actif) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.id(), tenant.nom(), tenant.pays(), tenant.actif());
    }
}
