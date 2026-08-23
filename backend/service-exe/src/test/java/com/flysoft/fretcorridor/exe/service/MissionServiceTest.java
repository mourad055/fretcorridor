package com.flysoft.fretcorridor.exe.service;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.entity.EtapeMission;
import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.entity.PreuveEtape;
import com.flysoft.fretcorridor.exe.messaging.MissionEventPublisher;
import com.flysoft.fretcorridor.exe.messaging.MissionLivreeEvent;
import com.flysoft.fretcorridor.exe.repository.EtapeMissionRepository;
import com.flysoft.fretcorridor.exe.repository.EtapeTourneeRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import com.flysoft.fretcorridor.exe.repository.PlanChargementEtapeRepository;
import com.flysoft.fretcorridor.exe.repository.PreuveEtapeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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
    @Mock private PlanChargementEtapeRepository planChargementEtapeRepository;
    @Mock private PreuveEtapeRepository preuveEtapeRepository;
    @Mock private PreuveStorageService preuveStorageService;
    @Mock private MissionEventPublisher missionEventPublisher;

    private MissionService service;
    private UUID missionId;
    private UUID transporteurId;
    private static final String TENANT = "tenant-bgft-douala";

    private MultipartFile unePhoto() {
        return new MockMultipartFile("photo", "photo.jpg", "image/jpeg", "contenu-photo".getBytes());
    }

    private MultipartFile uneSignature() {
        return new MockMultipartFile("signature", "signature.png", "image/png", "contenu-signature".getBytes());
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MissionService(missionRepository, etapeMissionRepository, preuveEtapeRepository,
                preuveStorageService, etapeTourneeRepository, planChargementEtapeRepository, missionEventPublisher);
        missionId = UUID.randomUUID();
        transporteurId = UUID.randomUUID();
        when(etapeMissionRepository.findByMissionIdOrderByHorodatageTransmissionAsc(any())).thenReturn(List.of());
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(etapeMissionRepository.save(any(EtapeMission.class))).thenAnswer(inv -> {
            EtapeMission e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(preuveStorageService.deposer(any(), any(), any(), any()))
                .thenReturn(new PreuveStorageService.ResultatDepot("cle-objet", "empreinte-sha256"));
        when(preuveEtapeRepository.save(any(PreuveEtape.class))).thenAnswer(inv -> inv.getArgument(0));
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

        var reponse = service.ajouterEtape(missionId, transporteurId, TENANT, requete,
                List.of(unePhoto()), uneSignature());

        assertThat(reponse.getStatut()).isEqualTo("PRISE_EN_CHARGE");
        verify(etapeMissionRepository).save(any(EtapeMission.class));
        verify(preuveEtapeRepository).save(any(PreuveEtape.class));
        verify(missionEventPublisher, never()).publierMissionLivree(any());
    }

    // RG-070/EF-EXE-03 (audit CDC du 19 août, bloquant corrigé) : une preuve
    // minimale (photo + signature tierce) est désormais exigée pour toute
    // PRISE_EN_CHARGE/LIVRAISON.
    @Test
    void a_pickup_stage_without_proof_is_refused() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.PRISE_EN_CHARGE);
        requete.setLibelle("Prise en charge à Douala");

        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete, null, null))
                .hasMessage("PREUVE_MANQUANTE");
        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete,
                List.of(unePhoto()), null))
                .hasMessage("PREUVE_MANQUANTE");
        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete,
                List.of(), uneSignature()))
                .hasMessage("PREUVE_MANQUANTE");
        verify(etapeMissionRepository, never()).save(any());
    }

    // Le chemin JSON (sans preuve) reste valable pour EN_TRANSIT/INCIDENT
    // (EF-EXE-03 ne les concerne pas), mais PRISE_EN_CHARGE/LIVRAISON y sont
    // désormais rejetées -- le mobile doit passer par l'endpoint multipart.
    @Test
    void the_json_only_overload_refuses_pickup_without_proof() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.PRISE_EN_CHARGE);
        requete.setLibelle("Prise en charge à Douala");

        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete))
                .hasMessage("PREUVE_MANQUANTE");
    }

    @Test
    void delivering_a_mission_already_picked_up_publishes_a_mission_livree_event() {
        Mission missionDejaPriseEnCharge = missionDuTransporteur();
        missionDejaPriseEnCharge.setStatut(Mission.StatutMission.PRISE_EN_CHARGE);
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDejaPriseEnCharge));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.LIVRAISON);
        requete.setLibelle("Livraison à Yaoundé");

        var reponse = service.ajouterEtape(missionId, transporteurId, TENANT, requete,
                List.of(unePhoto()), uneSignature());

        assertThat(reponse.getStatut()).isEqualTo("LIVREE");
        verify(preuveEtapeRepository).save(any(PreuveEtape.class));
        ArgumentCaptor<MissionLivreeEvent> captor = ArgumentCaptor.forClass(MissionLivreeEvent.class);
        verify(missionEventPublisher).publierMissionLivree(captor.capture());
        assertThat(captor.getValue().missionId()).isEqualTo(missionId);
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(captor.getValue().transporteurId()).isEqualTo(transporteurId);
        assertThat(captor.getValue().preuveLivraisonReference()).isNotBlank();
    }

    // RG-062 (audit CDC du 19 août, bloquant corrigé) : une livraison sans
    // prise en charge préalable libérait le séquestre à tort — reproduit
    // involontairement par l'ancienne version de ce test.
    @Test
    void delivering_a_mission_never_picked_up_is_refused() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(missionDuTransporteur()));

        var requete = new MissionDto.AjouterEtapeRequest();
        requete.setType(EtapeMission.TypeEtape.LIVRAISON);
        requete.setLibelle("Livraison à Yaoundé");

        assertThatThrownBy(() -> service.ajouterEtape(missionId, transporteurId, TENANT, requete))
                .hasMessage("ETAPE_HORS_SEQUENCE");

        verify(etapeMissionRepository, never()).save(any());
        verify(missionEventPublisher, never()).publierMissionLivree(any());
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

    /** S16/EF-MAT-13 (audit de suivi, 23 août) : la tournée expose la charge par essieu quand elle est connue. */
    @Test
    void getTournee_rattache_la_charge_par_essieu_connue_a_l_etape_du_meme_rang() {
        UUID tourneeId = UUID.randomUUID();
        UUID demandeId = UUID.randomUUID();
        Mission mission = Mission.builder().id(missionId).tenantId(TENANT).transporteurId(transporteurId)
                .statut(Mission.StatutMission.EN_TRANSIT).build();
        com.flysoft.fretcorridor.exe.entity.EtapeTournee etape =
                com.flysoft.fretcorridor.exe.entity.EtapeTournee.builder()
                        .tourneeId(tourneeId).missionId(missionId).rang(1)
                        .typeEtape(com.flysoft.fretcorridor.exe.entity.EtapeTournee.TypeEtapeTournee.ENLEVEMENT)
                        .demandeId(demandeId).pointLatitude(4.05).pointLongitude(9.7).build();
        Map<String, Object> chargesEssieu = Map.of("essieu_1", 3200.0, "essieu_2", 5800.0);
        com.flysoft.fretcorridor.exe.entity.PlanChargementEtape plan =
                com.flysoft.fretcorridor.exe.entity.PlanChargementEtape.builder()
                        .tourneeId(tourneeId).rang(1).chargesParEssieu(chargesEssieu)
                        .dateGeneration(java.time.LocalDateTime.now()).build();

        when(etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tourneeId)).thenReturn(List.of(etape));
        when(missionRepository.findAllById(List.of(missionId))).thenReturn(List.of(mission));
        when(planChargementEtapeRepository.findByTourneeId(tourneeId)).thenReturn(List.of(plan));

        MissionDto.TourneeResponse reponse = service.getTournee(tourneeId, transporteurId, TENANT);

        assertThat(reponse.getEtapes()).hasSize(1);
        assertThat(reponse.getEtapes().get(0).getChargesParEssieu()).isEqualTo(chargesEssieu);
    }

    /** Aucun plan de chargement ingéré pour cette tournée -> jamais une valeur inventée en remplacement. */
    @Test
    void getTournee_sans_plan_de_chargement_ingere_laisse_les_charges_par_essieu_a_null() {
        UUID tourneeId = UUID.randomUUID();
        UUID demandeId = UUID.randomUUID();
        Mission mission = Mission.builder().id(missionId).tenantId(TENANT).transporteurId(transporteurId)
                .statut(Mission.StatutMission.EN_TRANSIT).build();
        com.flysoft.fretcorridor.exe.entity.EtapeTournee etape =
                com.flysoft.fretcorridor.exe.entity.EtapeTournee.builder()
                        .tourneeId(tourneeId).missionId(missionId).rang(1)
                        .typeEtape(com.flysoft.fretcorridor.exe.entity.EtapeTournee.TypeEtapeTournee.ENLEVEMENT)
                        .demandeId(demandeId).pointLatitude(4.05).pointLongitude(9.7).build();

        when(etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tourneeId)).thenReturn(List.of(etape));
        when(missionRepository.findAllById(List.of(missionId))).thenReturn(List.of(mission));
        when(planChargementEtapeRepository.findByTourneeId(tourneeId)).thenReturn(List.of());

        MissionDto.TourneeResponse reponse = service.getTournee(tourneeId, transporteurId, TENANT);

        assertThat(reponse.getEtapes().get(0).getChargesParEssieu()).isNull();
    }
}
