package com.fretcorridor.adm.domain;

import java.util.List;
import java.util.Optional;

public interface TenantPort {
    void sauvegarder(Tenant tenant);

    Optional<Tenant> parId(String id);

    List<Tenant> lister();
}
