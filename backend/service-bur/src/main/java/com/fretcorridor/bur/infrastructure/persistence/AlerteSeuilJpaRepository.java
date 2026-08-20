package com.fretcorridor.bur.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlerteSeuilJpaRepository extends JpaRepository<AlerteSeuilEntity, String> {

    List<AlerteSeuilEntity> findByTenantId(String tenantId);

    void deleteByIdAndTenantId(String id, String tenantId);
}
