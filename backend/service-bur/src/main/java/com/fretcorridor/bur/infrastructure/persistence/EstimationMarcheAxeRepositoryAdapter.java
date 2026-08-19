package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.EstimationMarcheAxe;
import com.fretcorridor.bur.domain.EstimationMarcheAxePort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class EstimationMarcheAxeRepositoryAdapter implements EstimationMarcheAxePort {

    private final EstimationMarcheAxeJpaRepository jpaRepository;

    public EstimationMarcheAxeRepositoryAdapter(EstimationMarcheAxeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void definir(EstimationMarcheAxe estimation) {
        jpaRepository.save(new EstimationMarcheAxeEntity(estimation.tenantId(), estimation.axeId(),
                estimation.volumeMensuelEstime(), estimation.source(), estimation.definieParActeurId(),
                estimation.definieLe()));
    }

    @Override
    public Optional<EstimationMarcheAxe> pour(String tenantId, UUID axeId) {
        return jpaRepository.findByTenantIdAndAxeId(tenantId, axeId).map(this::versDomaine);
    }

    private EstimationMarcheAxe versDomaine(EstimationMarcheAxeEntity entity) {
        return new EstimationMarcheAxe(entity.getTenantId(), entity.getAxeId(), entity.getVolumeMensuelEstime(),
                entity.getSource(), entity.getDefinieParActeurId(), entity.getDefinieLe());
    }
}
