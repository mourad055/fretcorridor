package com.fretcorridor.pay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeclarationEspecesJpaRepository extends JpaRepository<DeclarationEspecesEntity, String> {

    Optional<DeclarationEspecesEntity> findByMissionId(String missionId);

    List<DeclarationEspecesEntity> findByTenantId(String tenantId);
}
