package com.flysoft.fretcorridor.exe.service;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.EtapeTournee;
import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.messaging.EtapeExecuteeEvent;
import com.flysoft.fretcorridor.exe.messaging.MissionEventPublisher;
import com.flysoft.fretcorridor.exe.messaging.MissionLivreeEvent;
import com.flysoft.fretcorridor.exe.repository.EtapeMissionRepository;
import com.flysoft.fretcorridor.exe.repository.EtapeTourneeRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final EtapeMissionRepository etapeMissionRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;
    private final MissionEventPublisher missionEventPublisher;

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
        etape = etapeMissionRepository.save(etape);

        nouveauStatutPour(requete.getType()).ifPresent(mission::setStatut);
        missionRepository.save(mission);

        publierEtapeExecuteeSiPertinent(mission, etape);

        if (requete.getType() == EtapeMission.TypeEtape.LIVRAISON) {
            publierMissionLivree(mission, etape);
        }

        var etapes = etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(missionId);
        return MissionDto.ChronologieResponse.fromEntity(mission, etapes);
    }

    // Item A (docs/DEPENDANCES_MOBILE_PHASE4.md) : déclenche la libération
    // du séquestre côté service-pay (RG-078) à la confirmation de livraison
    // — asynchrone, jamais d'appel REST synchrone inter-porteurs (Plan
    // d'exécution §4.2/4.3). mission.getTransporteurId() ne devrait jamais
    // être null ici (condition d'accès de missionAppartenantA), mais si le
    // chargeur n'existe déjà pas côté PAY, RG-078 dégrade sans bloquer —
    // pas la peine de vérifier deux fois.
    // S12 (EF-MAT-09/RG-058) : signale à OPT qu'une étape planifiée en
    // Tournee (S11, ENLEVEMENT/LIVRAISON) vient d'être réellement exécutée
    // — condition pour que le retour à vide se déclenche (jamais avant la
    // livraison effective de l'aller). EN_TRANSIT/INCIDENT n'ont pas
    // d'équivalent côté Tournee planifiée par OPT, rien à publier pour eux.
    private void publierEtapeExecuteeSiPertinent(Mission mission, EtapeMission etape) {
        EtapeExecuteeEvent.TypeEtape typeEtape = switch (etape.getType()) {
            case PRISE_EN_CHARGE -> EtapeExecuteeEvent.TypeEtape.ENLEVEMENT;
            case LIVRAISON -> EtapeExecuteeEvent.TypeEtape.LIVRAISON;
            case EN_TRANSIT, INCIDENT -> null;
        };
        if (typeEtape == null) {
            return;
        }
        missionEventPublisher.publierEtapeExecutee(new EtapeExecuteeEvent(
                UUID.randomUUID(),
                mission.getId(),
                typeEtape,
                etape.getHorodatageCapture().atZone(ZoneId.systemDefault()).toInstant()
        ));
    }

    private void publierMissionLivree(Mission mission, EtapeMission etapeLivraison) {
        missionEventPublisher.publierMissionLivree(new MissionLivreeEvent(
                UUID.randomUUID(),
                mission.getId(),
                mission.getTenantId(),
                mission.getTransporteurId(),
                etapeLivraison.getId().toString(),
                etapeLivraison.getHorodatageCapture().atZone(ZoneId.systemDefault()).toInstant()
        ));
    }

    // S11 : ordre planifié d'une Tournée consolidée, réservé au transporteur
    // qui possède au moins une des Missions qu'elle regroupe — même
    // discipline d'accès que missionAppartenantA, adaptée au fait qu'une
    // tournée porte plusieurs Missions (potentiellement d'autres
    // transporteurs si un jour la consolidation cross-transporteur existe,
    // hors périmètre S11 actuel).
    @Transactional(readOnly = true)
    public MissionDto.TourneeResponse getTournee(UUID tourneeId, UUID transporteurId, String tenantId) {
        List<EtapeTournee> etapes = etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tourneeId);
        if (etapes.isEmpty()) {
            throw new RuntimeException("MISSION_INTROUVABLE");
        }

        List<UUID> missionIds = etapes.stream().map(EtapeTournee::getMissionId).distinct().toList();
        List<Mission> missions = missionRepository.findAllById(missionIds);

        boolean transporteurConcerne = missions.stream().anyMatch(m ->
                tenantId.equals(m.getTenantId()) && transporteurId.equals(m.getTransporteurId()));
        if (!transporteurConcerne) {
            throw new RuntimeException("ACCES_REFUSE");
        }

        Map<UUID, String> statutParMission = missions.stream()
                .collect(Collectors.toMap(Mission::getId, m -> m.getStatut().name()));
        return MissionDto.TourneeResponse.of(tourneeId, etapes, statutParMission);
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
