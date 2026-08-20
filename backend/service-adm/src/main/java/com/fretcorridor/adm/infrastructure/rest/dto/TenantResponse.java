package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.Tenant;

public record TenantResponse(String id, String nom, String pays) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.id(), tenant.nom(), tenant.pays());
    }
}
