package com.fretcorridor.adm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalAuditJpaRepository extends JpaRepository<JournalAuditEntity, String> {
    List<JournalAuditEntity> findByTenantIdOrderByHorodatageDesc(String tenantId);

    List<JournalAuditEntity> findAllByOrderByHorodatageDesc();
}
