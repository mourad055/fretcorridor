package com.flysoft.fretcorridor.flt.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Meme pattern que MktEventPublisher/TrkEventPublisher : send() n'est pas
 * purement asynchrone (peut bloquer/lancer si les metadonnees du topic ne
 * sont pas encore en cache), donc le try/catch entoure l'appel send()
 * lui-meme - une panne Kafka ne doit jamais faire echouer l'ingestion d'une
 * position cote chauffeur (ENF-DIS-04).
 */
@Component
public class FltEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FltEventPublisher.class);
    private static final String TOPIC_POSITION_BRUTE = "position-brute";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FltEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierPositionBrute(PositionBruteEvent event) {
        try {
            kafkaTemplate.send(TOPIC_POSITION_BRUTE, event.missionId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("PositionBrute publiee - mission={}, offset={}",
                                    event.missionId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PositionBrute (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PositionBrute (send() bloquant) - mission={} - "
                    + "publication non bloquante pour le chauffeur (ENF-DIS-04)",
                    event.missionId(), exceptionBlocante);
        }
    }
}
