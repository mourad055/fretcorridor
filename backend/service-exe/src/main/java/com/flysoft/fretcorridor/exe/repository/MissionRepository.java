package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findByDemandeIdAndTenantId(UUID demandeId, String tenantId);
}
