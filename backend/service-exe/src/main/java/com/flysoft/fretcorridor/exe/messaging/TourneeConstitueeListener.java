package com.flysoft.fretcorridor.exe.messaging;

import com.flysoft.fretcorridor.exe.entity.EtapeTournee;
import com.flysoft.fretcorridor.exe.entity.Mission;
import com.flysoft.fretcorridor.exe.repository.EtapeTourneeRepository;
import com.flysoft.fretcorridor.exe.repository.MissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * S11 (EF-MAT-05/06) : consomme TourneeConstituee (service-opt, Moteur) pour
 * regrouper sous un même tourneeId les Missions déjà créées par
 * AffectationConfirmeeListener, et persister l'ordre planifié des étapes
 * (EtapeTournee) pour l'écran "tournée multi-étapes" (S11, Chauffeur).
 *
 * Contrat encore BROUILLON côté service-opt au 2026-08-19 (shared-contracts,
 * point de synchro hebdo à venir sur l'AsyncAPI) — implémentation
 * volontairement tolérante : une Mission pas encore ingérée (ordre Kafka
 * non garanti entre les topics affectation-confirmee et tournee-constituee)
 * est journalisée et ignorée pour ce passage, jamais bloquante (ENF-DIS-04),
 * rattrapable si l'événement est rejoué.
 *
 * Chaque save() reste sa propre transaction (pas de @Transactional de
 * méthode) : un doublon sur une étape ne doit jamais empêcher l'ingestion
 * des étapes suivantes du même événement (même raisonnement que
 * CapaciteDeclareeListener, service-opt).
 */
@Component
public class TourneeConstitueeListener {

    private static final Logger log = LoggerFactory.getLogger(TourneeConstitueeListener.class);

    private final MissionRepository missionRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;

    public TourneeConstitueeListener(MissionRepository missionRepository,
                                      EtapeTourneeRepository etapeTourneeRepository) {
        this.missionRepository = missionRepository;
        this.etapeTourneeRepository = etapeTourneeRepository;
    }

    @KafkaListener(topics = "tournee-constituee", containerFactory = "tourneeConstitueeKafkaListenerContainerFactory")
    public void ingerer(TourneeConstitueeEvent event) {
        for (EtapeConstitueeDto etape : event.etapes()) {
            rattacherMissionALaTournee(etape.missionId(), event.tourneeId());
            enregistrerEtape(event.tourneeId(), etape);
        }
        log.debug("TourneeConstituee ingérée - tournee={}, {} étape(s)", event.tourneeId(), event.etapes().size());
    }

    private void rattacherMissionALaTournee(java.util.UUID missionId, java.util.UUID tourneeId) {
        Optional<Mission> mission = missionRepository.findById(missionId);
        if (mission.isEmpty()) {
            log.warn("TourneeConstituee référence une Mission pas encore ingérée (ordre Kafka) - "
                    + "tourneeId={}, missionId={} - non bloquant, rattrapable au rejeu", tourneeId, missionId);
            return;
        }
        Mission m = mission.get();
        if (m.getTourneeId() == null) {
            m.setTourneeId(tourneeId);
            missionRepository.save(m);
        }
    }

    private void enregistrerEtape(java.util.UUID tourneeId, EtapeConstitueeDto etape) {
        try {
            etapeTourneeRepository.save(EtapeTournee.builder()
                    .tourneeId(tourneeId)
                    .missionId(etape.missionId())
                    .rang(etape.rang())
                    .typeEtape(EtapeTournee.TypeEtapeTournee.valueOf(etape.typeEtape()))
                    .demandeId(etape.demandeId())
                    .pointLatitude(etape.pointLatitude())
                    .pointLongitude(etape.pointLongitude())
                    .fenetreDebut(versLocalDateTime(etape.fenetreDebut()))
                    .fenetreFin(versLocalDateTime(etape.fenetreFin()))
                    .build());
        } catch (DataIntegrityViolationException doublon) {
            log.debug("EtapeTournee déjà ingérée, doublon ignoré - tournee={}, rang={}", tourneeId, etape.rang());
        }
    }

    private LocalDateTime versLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
