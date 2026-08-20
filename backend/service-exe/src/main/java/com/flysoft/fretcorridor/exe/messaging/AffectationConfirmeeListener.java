package com.flysoft.fretcorridor.exe.messaging;

import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme AffectationConfirmee (service-opt, Moteur) pour créer la mission
 * côté chauffeur (S7, EF-EXE-02) — jusqu'ici seul service-bur consommait cet
 * événement (vue Bureau). Idempotent par construction : Mission.id EST le
 * missionId de l'événement (clé primaire), un rejeu est un no-op silencieux.
 *
 * {@code tenantIdPhase1} : même hypothèse mono-tenant que service-bur —
 * l'événement ne porte aucun tenant (cf. ADR 0011, GEO).
 */
@Component
public class AffectationConfirmeeListener {

    private static final Logger log = LoggerFactory.getLogger(AffectationConfirmeeListener.class);

    private final MissionRepository missionRepository;
    private final String tenantIdPhase1;

    public AffectationConfirmeeListener(MissionRepository missionRepository,
                                         @Value("${fretcorridor.exe.tenant-id-phase1}") String tenantIdPhase1) {
        this.missionRepository = missionRepository;
        this.tenantIdPhase1 = tenantIdPhase1;
    }

    @KafkaListener(topics = "affectation-confirmee", containerFactory = "affectationConfirmeeKafkaListenerContainerFactory")
    public void ingerer(AffectationConfirmeeEvent event) {
        if (missionRepository.existsById(event.missionId())) {
            log.debug("AffectationConfirmee déjà ingérée, doublon ignoré - missionId={}", event.missionId());
            return;
        }

        Mission mission = Mission.builder()
                .id(event.missionId())
                .demandeId(event.demandeId())
                .transporteurId(event.transporteurId())
                .vehiculeId(event.vehiculeId())
                .axeId(event.axeId())
                .origineNom(event.origineNom())
                .destinationNom(event.destinationNom())
                .statut(Mission.StatutMission.EN_ATTENTE)
                .tenantId(tenantIdPhase1)
                .build();
        missionRepository.save(mission);
        log.debug("Mission créée côté exécution - mission={}, transporteur={}", event.missionId(), event.transporteurId());
    }
}
