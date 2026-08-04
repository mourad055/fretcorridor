package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.MissionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MissionRepositoryAdapter implements MissionRepositoryPort {

    private final MissionEnregistrementJpaRepository jpaRepository;

    public MissionRepositoryAdapter(MissionEnregistrementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void enregistrer(String tenantId, String axeId) {
        jpaRepository.save(new MissionEnregistrementEntity(tenantId, axeId, Instant.now()));
    }

    @Override
    public long compter(String tenantId, String axeId) {
        return jpaRepository.countByTenantIdAndAxeId(tenantId, axeId);
    }
}
