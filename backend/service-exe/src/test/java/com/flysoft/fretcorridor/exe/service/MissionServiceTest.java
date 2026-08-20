package com.flysoft.fretcorridor.exe.service;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.messaging.MissionEventPublisher;
import com.flysoft.fretcorridor.exe.messaging.MissionLivreeEvent;
import com.flysoft.fretcorridor.exe.repository.EtapeMissionRepository;
import com.flysoft.fretcorridor.exe.repository.EtapeTourneeRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MissionServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private EtapeMissionRepository etapeMissionRepository;
    @Mock private EtapeTourneeRepository etapeTourneeRepository;
    @Mock private MissionEventPublisher missionEventPublisher;

    private MissionService service;
    private UUID missionId;
    private UUID transporteurId;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MissionService(missionRepository, etapeMissionRepository, etapeTourneeRepository, missionEventPublisher);
        missionId = UUID.randomUUID();
        transporteurId = UUID.randomUUID();
        when(etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(any())).thenReturn(List.of());
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(etapeMissionRepository.save(any(EtapeMission.class))).thenAnswer(inv -> {
            EtapeMission e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
    }

    private Mission missionDuTransporteur() {
        return Mission.builder().id(missionId).demandeId(UUID.randomUUID()).transporteurId(transporteurId)
                .tenantId(TENANT).statut(Mission.StatutMission.EN_ATTENTE).build();
    }

    @Test
    void a_driver_only_sees_their_own_missions() {
        when(missionRepository.findByTransporteurIdAndTenantIdOrderByDateCreationDesc(transporteurId, TENANT))
                .thenReturn(List.of(missionDuTransporteur()));

        var missions = service.listerMesMissions(transporteurId, TENANT);

        assertThat(missions).hasSize(1);
        assertThat(missions.get(0).getMissionId()).isEqualTo(missionId);
    }

    @Test
    void adding_a_pickup_stage_advances_the_mission_status() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.PRISE_EN_CHARGE);
        requete.setLibelle("Prise en charge à Douala");

        var reponse = service.ajouterEtape(missionId, transporteurId, TENANT, requete);

        assertThat(reponse.getStatut()).isEqualTo("PRISE_EN_CHARGE");
        verify(etapeMissionRepository).save(any(EtapeMission.class));
        verify(missionEventPublisher, never()).publierMissionLivree(any());
    }

    @Test
    void delivering_a_mission_publishes_a_mission_livree_event() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.LIVRAISON);
        requete.setLibelle("Livraison à Yaoundé");

        var reponse = service.ajouterEtape(missionId, transporteurId, TENANT, requete);

        assertThat(reponse.getStatut()).isEqualTo("LIVREE");
        ArgumentCaptor<MissionLivreeEvent> captor = ArgumentCaptor.forClass(MissionLivreeEvent.class);
        verify(missionEventPublisher).publierMissionLivree(captor.capture());
        assertThat(captor.getValue().missionId()).isEqualTo(missionId);
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(captor.getValue().transporteurId()).isEqualTo(transporteurId);
        assertThat(captor.getValue().preuveLivraisonReference()).isNotBlank();
    }

    @Test
    void an_incident_is_logged_without_changing_the_mission_status() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.INCIDENT);
        requete.setLibelle("Panne moteur");

        var reponse = service.ajouterEtape(missionId, transporteurId, TENANT, requete);

        assertThat(reponse.getStatut()).isEqualTo("EN_ATTENTE");
    }

    @Test
    void a_driver_cannot_add_a_stage_to_someone_elses_mission() {
        UUID autreTransporteur = UUID.randomUUID();
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.PRISE_EN_CHARGE);
        requete.setLibelle("Prise en charge");

        assertThatThrownBy(() -> service.ajouterEtape(missionId, autreTransporteur, TENANT, requete))
                .hasMessage("ACCES_REFUSE");

        verify(etapeMissionRepository, never()).save(any());
    }

    @Test
    void an_unknown_mission_is_reported_as_not_found() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.empty());

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.PRISE_EN_CHARGE);
        requete.setLibelle("Prise en charge");

        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete))
                .hasMessage("MISSION_INTROUVABLE");
    }
}
