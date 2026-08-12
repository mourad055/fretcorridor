package com.flysoft.fretcorridor.exe.service;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.repository.EtapeMissionRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // S7 : missions du chauffeur/transporteur connecté (cf. AffectationConfirmeeListener)
    @Transactional(readOnly = true)
    public List<MissionDto.MissionResumeResponse> listerMesMissions(UUID transporteurId, String tenantId) {
        return missionRepository.findByTransporteurIdAndTenantIdOrderByDateCreationDesc(transporteurId, tenantId)
                .stream().map(MissionDto.MissionResumeResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MissionDto.ChronologieResponse getChronologie(UUID missionId, UUID transporteurId, String tenantId) {
        Mission mission = missionAppartenantA(missionId, transporteurId, tenantId);
        var etapes = etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(missionId);
        return MissionDto.ChronologieResponse.fromEntity(mission, etapes);
    }

    // EF-EXE-02/04 : une étape fait progresser le statut de la mission —
    // INCIDENT est journalisé sans changer le statut (pas une étape terminale).
    @Transactional
    public MissionDto.ChronologieResponse ajouterEtape(UUID missionId, UUID transporteurId, String tenantId,
                                                         MissionDto.AjouterEtapeRequest requete) {
        Mission mission = missionAppartenantA(missionId, transporteurId, tenantId);

        EtapeMission etape = EtapeMission.builder()
                .mission(mission)
                .type(requete.getType())
                .libelle(requete.getLibelle())
                .horodatageCapture(requete.getHorodatageCapture() != null ? requete.getHorodatageCapture() : LocalDateTime.now())
                .build();
        etapeMissionRepository.save(etape);

        nouveauStatutPour(requete.getType()).ifPresent(mission::setStatut);
        missionRepository.save(mission);

        var etapes = etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(missionId);
        return MissionDto.ChronologieResponse.fromEntity(mission, etapes);
    }

    private Mission missionAppartenantA(UUID missionId, UUID transporteurId, String tenantId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("MISSION_INTROUVABLE"));
        if (!mission.getTenantId().equals(tenantId) || !mission.getId().equals(missionId)
                || mission.getTransporteurId() == null || !mission.getTransporteurId().equals(transporteurId)) {
            throw new RuntimeException("ACCES_REFUSE");
        }
        return mission;
    }

    private Optional<Mission.StatutMission> nouveauStatutPour(EtapeMission.TypeEtape type) {
        return switch (type) {
            case PRISE_EN_CHARGE -> Optional.of(Mission.StatutMission.PRISE_EN_CHARGE);
            case EN_TRANSIT -> Optional.of(Mission.StatutMission.EN_TRANSIT);
            case LIVRAISON -> Optional.of(Mission.StatutMission.LIVREE);
            case INCIDENT -> Optional.empty();
        };
    }
}
