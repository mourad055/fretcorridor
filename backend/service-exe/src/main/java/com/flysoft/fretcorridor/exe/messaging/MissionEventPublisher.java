package com.flysoft.fretcorridor.exe.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MissionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MissionEventPublisher.class);
    private static final String TOPIC_MISSION_LIVREE = "mission-livree";
    private static final String TOPIC_ETAPE_EXECUTEE = "etape-executee";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MissionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Degradation gracieuse : un echec de publication ne doit jamais faire
    // echouer la confirmation de livraison cote chauffeur (ENF-DIS-04) —
    // meme raisonnement que OptEventPublisher (service-opt).
    public void publierMissionLivree(MissionLivreeEvent event) {
        try {
            kafkaTemplate.send(TOPIC_MISSION_LIVREE, event.missionId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("MissionLivree publiee - mission={}, offset={}",
                                    event.missionId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication MissionLivree (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication MissionLivree (send() bloquant) - mission={} - "
                    + "confirmation de livraison non bloquee (ENF-DIS-04)", event.missionId(), exceptionBlocante);
        }
    }

    // S12 (EF-MAT-09/RG-058) : meme degradation gracieuse - un echec de
    // publication ne doit jamais bloquer la confirmation d'etape cote
    // chauffeur.
    public void publierEtapeExecutee(EtapeExecuteeEvent event) {
        try {
            kafkaTemplate.send(TOPIC_ETAPE_EXECUTEE, event.missionId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("EtapeExecutee publiee - mission={}, typeEtape={}, offset={}",
                                    event.missionId(), event.typeEtape(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication EtapeExecutee (callback async) - mission={}, typeEtape={}",
                                    event.missionId(), event.typeEtape(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication EtapeExecutee (send() bloquant) - mission={}, typeEtape={} - "
                    + "confirmation d'etape non bloquee (ENF-DIS-04)", event.missionId(), event.typeEtape(),
                    exceptionBlocante);
        }
    }
}
