package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.dto.PositionDto;
import com.flysoft.fretcorridor.flt.entity.Position;
import com.flysoft.fretcorridor.flt.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    // Appelée par l'app Chauffeur/Transporteur (pas encore construite) —
    // exposée dès maintenant pour que le contrat d'API soit stable (§ conventions du dépôt).
    @Transactional
    public void enregistrer(PositionDto.EnvoyerRequest request, String tenantId) {
        Position position = Position.builder()
                .missionId(request.getMissionId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .horodatage(request.getHorodatage())
                .tenantId(tenantId)
                .build();
        positionRepository.save(position);
    }

    // Consommée par l'app Client (S6) — peut renvoyer "absent" tant qu'aucun
    // chauffeur n'envoie de position pour cette mission.
    @Transactional(readOnly = true)
    public Optional<PositionDto.DernierePositionResponse> getDernierePosition(UUID missionId, String tenantId) {
        return positionRepository.findFirstByMissionIdAndTenantIdOrderByHorodatageDesc(missionId, tenantId)
                .map(PositionDto.DernierePositionResponse::fromEntity);
    }
}
