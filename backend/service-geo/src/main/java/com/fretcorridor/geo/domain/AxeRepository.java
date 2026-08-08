package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AxeRepository extends JpaRepository<Axe, UUID> {

    /**
     * Axes ou le matching est actif (EF-GEO-03) : c'est cette liste que
     * consommera OPT (filtre L0) une fois l'appel synchrone interne en place.
     */
    List<Axe> findByMatchingActifTrue();
}
