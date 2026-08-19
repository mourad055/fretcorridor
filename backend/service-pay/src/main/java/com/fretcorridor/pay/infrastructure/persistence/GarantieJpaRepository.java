package com.fretcorridor.pay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarantieJpaRepository extends JpaRepository<GarantieEntity, String> {

    Optional<GarantieEntity> findByMissionId(String missionId);
}
