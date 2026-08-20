package com.fretcorridor.pay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EcritureMiroirJpaRepository extends JpaRepository<EcritureMiroirEntity, String> {

    List<EcritureMiroirEntity> findByMissionId(String missionId);

    List<EcritureMiroirEntity> findByBeneficiaireId(String beneficiaireId);

    List<EcritureMiroirEntity> findByTenantId(String tenantId);

    @Query("SELECT DISTINCT e.missionId FROM EcritureMiroirEntity e")
    List<String> findDistinctMissionIds();
}
