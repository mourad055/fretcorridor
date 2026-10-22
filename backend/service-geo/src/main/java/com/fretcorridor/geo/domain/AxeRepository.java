package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
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
    List<Axe> findByTenantId(String tenantId);

    /**
     * Resolution GPS -> axe (GET /api/geo/axes/resoudre) : cherche l'axe
     * oriente reliant deux hubs donnes, uniquement s'il est actif pour le
     * matching (un axe absent de ce resultat ne doit jamais etre propose).
     * Le filtrage tenant, quand il est fourni, est applique par
     * AxeResolutionService (au plus quelques axes par paire de hubs).
     */
    Optional<Axe> findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
            UUID hubOrigineId, UUID hubDestinationId);
}
