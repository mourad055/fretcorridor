package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AxeRepository extends JpaRepository<Axe, UUID> {

    /**
     * Axes ou le matching est actif (EF-GEO-03) : c'est cette liste que
     * consomme OPT (filtre L0), en synchrone interne.
     */
    List<Axe> findByMatchingActifTrue();

    /**
     * ENF-MUL-01 : isolation stricte par tenant, filtree ICI en base -
     * jamais fabriquee a posteriori par un appelant (gateway ou autre).
     */
    List<Axe> findByTenantId(UUID tenantId);
}
