package com.fretcorridor.opt.oracle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanChargementRepository extends JpaRepository<PlanChargement, UUID> {

    /** Tous les etats intermediaires verifies pour une tournee, dans l'ordre de creation. */
    List<PlanChargement> findByTourneeIdOrderByDateCreationAsc(UUID tourneeId);
}
