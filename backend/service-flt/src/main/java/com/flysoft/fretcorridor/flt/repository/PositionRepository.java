package com.flysoft.fretcorridor.flt.repository;

import com.flysoft.fretcorridor.flt.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    Optional<Position> findFirstByMissionIdOrderByHorodatageDesc(UUID missionId);
}
