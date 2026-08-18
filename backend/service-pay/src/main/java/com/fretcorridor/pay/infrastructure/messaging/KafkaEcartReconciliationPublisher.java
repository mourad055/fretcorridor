package com.fretcorridor.pay.infrastructure.messaging;

import com.fretcorridor.pay.domain.ReconciliationEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * EF-PAY-09 : même patron que KafkaDossierEventPublisher côté service-adm
 * (sens inverse) — send() n'est pas purement asynchrone, le try/catch entoure
 * l'appel lui-même pour qu'une panne Kafka ne fasse jamais échouer la
 * réconciliation elle-même (ENF-DIS-04) : les écritures restent isolées
 * localement même si l'alerte ne part pas.
 */
@Component
public class KafkaEcartReconciliationPublisher implements ReconciliationEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEcartReconciliationPublisher.class);
    private static final String TOPIC_ECART_RECONCILIATION = "ecart-reconciliation";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEcartReconciliationPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publier(String missionId, String tenantId, BigDecimal ecart, Instant declencheeLe) {
        EcartReconciliationEvent event = new EcartReconciliationEvent(
                UUID.randomUUID().toString(), missionId, tenantId, ecart, declencheeLe);

        try {
            kafkaTemplate.send(TOPIC_ECART_RECONCILIATION, missionId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("EcartReconciliation publié - mission={}, ecart={}, offset={}",
                                    missionId, ecart, result.getRecordMetadata().offset());
                        } else {
                            log.error("Échec publication EcartReconciliation (callback async) - mission={}", missionId, ex);
                        }
                    });
        } catch (Exception exceptionBloquante) {
            log.error("Échec publication EcartReconciliation (send() bloquant) - mission={} - "
                    + "réconciliation non bloquée pour autant (ENF-DIS-04)", missionId, exceptionBloquante);
        }
    }
}
