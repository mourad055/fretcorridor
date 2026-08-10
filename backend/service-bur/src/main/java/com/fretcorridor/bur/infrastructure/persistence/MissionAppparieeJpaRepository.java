package com.fretcorridor.bur.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MissionAppparieeJpaRepository extends JpaRepository<MissionAppparieeEntity, UUID> {

    List<MissionAppparieeEntity> findByTenantIdOrderByConfirmeeLeDesc(String tenantId);
}
