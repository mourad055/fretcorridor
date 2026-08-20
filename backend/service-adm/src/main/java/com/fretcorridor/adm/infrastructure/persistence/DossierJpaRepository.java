package com.fretcorridor.adm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DossierJpaRepository extends JpaRepository<DossierEntity, String> {
    List<DossierEntity> findByTenantId(String tenantId);
}
