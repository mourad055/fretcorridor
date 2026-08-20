package com.flysoft.fretcorridor.cap.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CapEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CapEventPublisher.class);
    private static final String TOPIC = "capacite-declaree";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CapEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierCapaciteDeclaree(CapaciteDeclareeEvent event) {
        kafkaTemplate.send(TOPIC, event.capaciteId().toString(), event)
                .whenComplete((resultat, exception) -> {
                    if (exception != null) {
                        log.error("Echec publication CapaciteDeclaree - capacite={} : {}",
                                event.capaciteId(), exception.getMessage());
                    } else {
                        log.debug("CapaciteDeclaree publiee - capacite={}, offset={}",
                                event.capaciteId(), resultat.getRecordMetadata().offset());
                    }
                });
    }
}
