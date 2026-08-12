package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findByDemandeIdAndTenantId(UUID demandeId, String tenantId);

    // S7 : reste vide en pratique tant que transporteurId n'est pas peuplé
    // en amont (AffectationConfirmee le porte toujours à null aujourd'hui,
    // cf. AffectationL1Service côté service-opt — écart documenté).
    List<Mission> findByTransporteurIdAndTenantIdOrderByDateCreationDesc(UUID transporteurId, String tenantId);
}
