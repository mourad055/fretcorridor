package com.fretcorridor.opt.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OptEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OptEventPublisher.class);
    private static final String TOPIC_PROPOSITION_EMISE = "proposition-emise";
    private static final String TOPIC_AFFECTATION_CONFIRMEE = "affectation-confirmee";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OptEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierPropositionEmise(PropositionEmiseEvent event) {
        String cle = event.demandeId().toString();
        kafkaTemplate.send(TOPIC_PROPOSITION_EMISE, cle, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("PropositionEmise publiee - demande={}, rang={}, offset={}",
                                event.demandeId(), event.rang(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Echec publication PropositionEmise - demande={}", event.demandeId(), ex);
                    }
                });
    }

    public void publierAffectationConfirmee(AffectationConfirmeeEvent event) {
        String cle = event.missionId().toString();
        kafkaTemplate.send(TOPIC_AFFECTATION_CONFIRMEE, cle, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("AffectationConfirmee publiee - mission={}, offset={}",
                                event.missionId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Echec publication AffectationConfirmee - mission={}", event.missionId(), ex);
                    }
                });
    }
}
