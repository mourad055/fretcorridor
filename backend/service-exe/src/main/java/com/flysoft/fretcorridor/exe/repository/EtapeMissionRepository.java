package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EtapeMissionRepository extends JpaRepository<EtapeMission, UUID> {
    List<EtapeMission> findByMissionIdOrderByHorodatageTransmissionAsc(UUID missionId);
}
