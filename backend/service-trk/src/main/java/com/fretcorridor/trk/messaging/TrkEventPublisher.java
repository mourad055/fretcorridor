package com.fretcorridor.trk.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrkEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TrkEventPublisher.class);
    private static final String TOPIC_POSITION_ETA = "position-eta";
    private static final String TOPIC_ALERTE_ECART = "alerte-ecart";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TrkEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierPositionEta(PositionEtaEvent event) {
        try {
            String cle = event.missionId().toString();
            kafkaTemplate.send(TOPIC_POSITION_ETA, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("PositionETA publiée - mission={}, eventId={}, offset={}",
                                    event.missionId(), event.eventId(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Échec publication PositionETA - mission={}, eventId={}",
                                    event.missionId(), event.eventId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Échec publication PositionETA (send() bloquant) - mission={}",
                    event.missionId(), e);
        }
    }

    public void publierAlerteEcart(AlerteEcartEvent event) {
        try {
            String cle = event.missionId().toString();
            kafkaTemplate.send(TOPIC_ALERTE_ECART, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("AlerteEcart publiée - mission={}, type={}, offset={}",
                                    event.missionId(), event.typeAnomalie(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Échec publication AlerteEcart - mission={}, type={}",
                                    event.missionId(), event.typeAnomalie(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Échec publication AlerteEcart (send() bloquant) - mission={}",
                    event.missionId(), e);
        }
    }
}
