package com.fretcorridor.opt.sequencement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EtapeTourneeRepository extends JpaRepository<EtapeTournee, UUID> {
    List<EtapeTournee> findByTourneeIdOrderByRangAsc(UUID tourneeId);
    boolean existsByAffectationId(UUID affectationId);

    // Sprint 12 (EtapeExecuteeListener) : localise l'etape a marquer
    // executee depuis un evenement externe qui ne connait que
    // affectationId (= missionId) + typeEtape, jamais l'id interne
    // EtapeTournee. Optional : l'evenement peut arriver avant que
    // l'affectation correspondante ait ete sequencee en Tournee (Sprint 11
    // pas encore declenche sur ce cycle), ou apres une replanification qui
    // aurait change les rangs - jamais suppose present.
    java.util.Optional<EtapeTournee> findByAffectationIdAndTypeEtape(
            UUID affectationId, EtapeTournee.TypeEtape typeEtape);
}
