package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropositionMissionRepository extends JpaRepository<PropositionMission, UUID> {
    List<PropositionMission> findByTransporteurIdAndStatutOrderByDateCreationDesc(
            UUID transporteurId, PropositionMission.Statut statut);
}
