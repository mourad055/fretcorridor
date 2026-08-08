package com.flysoft.fretcorridor.mkt.repository;

import com.flysoft.fretcorridor.mkt.entity.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, UUID> {
    List<Demande> findByClientActeurIdAndTenantIdOrderByDateCreationDesc(UUID clientActeurId, String tenantId);
    Optional<Demande> findByIdAndTenantId(UUID id, String tenantId);
}
