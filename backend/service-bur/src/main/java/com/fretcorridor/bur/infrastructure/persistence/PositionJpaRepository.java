package com.fretcorridor.bur.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID> {

    Optional<PositionEntity> findByMissionId(UUID missionId);

    List<PositionEntity> findByTenantIdOrderByCapturedLeDesc(String tenantId);
}
