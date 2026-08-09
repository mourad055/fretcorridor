package com.flysoft.fretcorridor.mkt.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Meme pattern que OptEventPublisher (service-opt) : send() n'est pas
 * purement asynchrone (peut bloquer/lancer si les metadonnees du topic ne
 * sont pas encore en cache), donc le try/catch entoure l'appel send()
 * lui-meme, pas seulement son whenComplete() - une panne Kafka ne doit
 * jamais faire echouer la publication d'une demande cote client (ENF-DIS-04).
 */
@Component
public class MktEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MktEventPublisher.class);
    private static final String TOPIC_DEMANDE_PUBLIEE = "demande-publiee";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MktEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierDemandePubliee(DemandePublieeEvent event) {
        try {
            kafkaTemplate.send(TOPIC_DEMANDE_PUBLIEE, event.demandeId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("DemandePubliee publiee - demande={}, offset={}",
                                    event.demandeId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication DemandePubliee (callback async) - demande={}",
                                    event.demandeId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication DemandePubliee (send() bloquant) - demande={} - "
                    + "publication non bloquante pour le client (ENF-DIS-04)",
                    event.demandeId(), exceptionBlocante);
        }
    }
}
