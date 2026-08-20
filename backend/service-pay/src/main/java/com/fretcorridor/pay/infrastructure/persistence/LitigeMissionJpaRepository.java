package com.fretcorridor.pay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LitigeMissionJpaRepository extends JpaRepository<LitigeMissionEntity, String> {

    Optional<LitigeMissionEntity> findByMissionId(String missionId);
}
