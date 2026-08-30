package com.fretcorridor.trk.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ColisRecuperationRepository extends JpaRepository<ColisRecuperation, UUID> {

    Optional<ColisRecuperation> findFirstByMissionId(UUID missionId);
}
