package com.flysoft.fretcorridor.cap.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CapEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CapEventPublisher.class);
    private static final String TOPIC = "capacite-declaree";
    private static final String TOPIC_DEMANDE_ACCEPTEE = "demande-acceptee";
    private static final String TOPIC_DEMANDE_REFUSEE = "demande-refusee-par-chauffeur";

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

    // UC-MAT-02/diffusion-course : cle de partition = affectationId, coherent
    // avec la resolution de course cote OPT (AffectationRepository.confirmerSiProposee),
    // pas transporteurId -- l'ordre entre deux transporteurs differents n'a
    // aucune importance, seul l'ordre des evenements sur UNE MEME affectation
    // pourrait en avoir (partition unique garantit cet ordre).
    public void publierDemandeAcceptee(DemandeAccepteeEvent event) {
        kafkaTemplate.send(TOPIC_DEMANDE_ACCEPTEE, event.affectationId().toString(), event)
                .whenComplete((resultat, exception) -> {
                    if (exception != null) {
                        log.error("Echec publication DemandeAcceptee - affectation={} : {}",
                                event.affectationId(), exception.getMessage());
                    } else {
                        log.debug("DemandeAcceptee publiee - affectation={}, offset={}",
                                event.affectationId(), resultat.getRecordMetadata().offset());
                    }
                });
    }

    public void publierDemandeRefuseeParChauffeur(DemandeRefuseeParChauffeurEvent event) {
        kafkaTemplate.send(TOPIC_DEMANDE_REFUSEE, event.affectationId().toString(), event)
                .whenComplete((resultat, exception) -> {
                    if (exception != null) {
                        log.error("Echec publication DemandeRefuseeParChauffeur - affectation={} : {}",
                                event.affectationId(), exception.getMessage());
                    } else {
                        log.debug("DemandeRefuseeParChauffeur publiee - affectation={}, offset={}",
                                event.affectationId(), resultat.getRecordMetadata().offset());
                    }
                });
    }
}
