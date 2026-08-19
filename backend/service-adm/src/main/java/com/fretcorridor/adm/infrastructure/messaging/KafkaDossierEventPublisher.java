package com.fretcorridor.adm.infrastructure.messaging;

import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.DossierEventPort;
import com.fretcorridor.adm.domain.StatutDossier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Le domaine (FileTravailService/DecisionService) n'appelle
 * {@link #publier(Dossier)} que pour les dossiers LITIGE rattachés à une
 * mission (EF-PAY-08) — cet adaptateur n'a pas de filtre à refaire. Même
 * pattern que MktEventPublisher/OptEventPublisher : send() n'est pas
 * purement asynchrone, le try/catch entoure l'appel lui-même pour qu'une
 * panne Kafka ne fasse jamais échouer la décision d'un agent Admin
 * (ENF-DIS-04).
 */
@Component
public class KafkaDossierEventPublisher implements DossierEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaDossierEventPublisher.class);
    private static final String TOPIC_DOSSIER_LITIGE = "dossier-litige";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaDossierEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publier(Dossier dossier) {
        boolean actif = dossier.statut() != StatutDossier.CLOS;
        Instant horodatage = actif ? dossier.ouvertLe() : dossier.decideLe();
        DossierLitigeEvent event = new DossierLitigeEvent(
                UUID.randomUUID().toString(), dossier.id(), dossier.tenantId(), dossier.missionId(), actif, horodatage);

        try {
            kafkaTemplate.send(TOPIC_DOSSIER_LITIGE, event.missionId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("DossierLitige publié - mission={}, actif={}, offset={}",
                                    event.missionId(), event.actif(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Échec publication DossierLitige (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBloquante) {
            log.error("Échec publication DossierLitige (send() bloquant) - mission={} - "
                    + "publication non bloquante pour l'agent Admin (ENF-DIS-04)",
                    event.missionId(), exceptionBloquante);
        }
    }
}
