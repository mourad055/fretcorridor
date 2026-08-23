package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.PlanChargementEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlanChargementEtapeRepository extends JpaRepository<PlanChargementEtape, UUID> {
    List<PlanChargementEtape> findByTourneeId(UUID tourneeId);
}
