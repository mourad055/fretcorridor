package com.fretcorridor.bur.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MissionEnregistrementJpaRepository extends JpaRepository<MissionEnregistrementEntity, UUID> {

    long countByTenantIdAndAxeId(String tenantId, String axeId);
}
