package com.flysoft.fretcorridor.exe.service;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.repository.EtapeMissionRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final EtapeMissionRepository etapeMissionRepository;

    // Consommé par l'app Client (S7) — absent tant qu'aucune mission n'a été
    // créée pour cette demande (le matching V0 côté Moteur est encore un stub).
    @Transactional(readOnly = true)
    public Optional<MissionDto.ChronologieResponse> getChronologiePourDemande(UUID demandeId, String tenantId) {
        return missionRepository.findByDemandeIdAndTenantId(demandeId, tenantId)
                .map(mission -> {
                    var etapes = etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(mission.getId());
                    return MissionDto.ChronologieResponse.fromEntity(mission, etapes);
                });
    }
}
