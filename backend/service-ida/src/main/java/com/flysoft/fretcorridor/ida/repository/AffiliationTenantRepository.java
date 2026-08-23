package com.flysoft.fretcorridor.ida.repository;

import com.flysoft.fretcorridor.ida.entity.AffiliationTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AffiliationTenantRepository extends JpaRepository<AffiliationTenant, UUID> {
    List<AffiliationTenant> findByActeurId(UUID acteurId);
    boolean existsByActeurIdAndTenantId(UUID acteurId, String tenantId);
}
