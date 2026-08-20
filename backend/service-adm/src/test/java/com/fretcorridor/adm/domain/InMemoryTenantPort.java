package com.fretcorridor.adm.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryTenantPort implements TenantPort {

    private final Map<String, Tenant> tenants = new LinkedHashMap<>();

    @Override
    public void sauvegarder(Tenant tenant) {
        tenants.put(tenant.id(), tenant);
    }

    @Override
    public Optional<Tenant> parId(String id) {
        return Optional.ofNullable(tenants.get(id));
    }

    @Override
    public List<Tenant> lister() {
        return List.copyOf(tenants.values());
    }
}
