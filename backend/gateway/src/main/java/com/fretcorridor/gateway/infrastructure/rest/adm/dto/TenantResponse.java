package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import com.fretcorridor.gateway.domain.adm.TenantVue;

public record TenantResponse(String id, String nom, String pays, boolean actif) {
    public static TenantResponse from(TenantVue tenant) {
        return new TenantResponse(tenant.id(), tenant.nom(), tenant.pays(), tenant.actif());
    }
}
