package com.fretcorridor.trk.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    @Query("SELECT p FROM Position p WHERE p.missionId = :missionId ORDER BY p.horodatageCapture ASC")
    List<Position> findByMissionIdOrderByHorodatageCaptureAsc(@Param("missionId") UUID missionId);
}
