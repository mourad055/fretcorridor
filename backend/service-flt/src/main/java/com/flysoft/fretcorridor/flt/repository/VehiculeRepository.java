package com.flysoft.fretcorridor.flt.repository;

import com.flysoft.fretcorridor.flt.entity.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VehiculeRepository extends JpaRepository<Vehicule, UUID> {
    List<Vehicule> findByProprietaireActeurIdAndTenantIdOrderByDateCreationDesc(UUID proprietaireActeurId, String tenantId);
}
