package com.fretcorridor.adm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, String> {
}
