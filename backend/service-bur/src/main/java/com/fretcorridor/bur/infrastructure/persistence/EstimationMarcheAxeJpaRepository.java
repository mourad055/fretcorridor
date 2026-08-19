package com.fretcorridor.bur.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstimationMarcheAxeJpaRepository
        extends JpaRepository<EstimationMarcheAxeEntity, EstimationMarcheAxeEntity.Cle> {

    Optional<EstimationMarcheAxeEntity> findByTenantIdAndAxeId(String tenantId, UUID axeId);
}
