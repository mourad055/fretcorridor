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
        // ATTENTION (piege classique KafkaTemplate) : send() N'EST PAS purement
        // asynchrone - l'appel bloque le thread appelant jusqu'a max.block.ms
        // (defaut 60s) si les metadonnees du topic ne sont pas encore en cache,
        // et LANCE l'exception directement (avant meme de retourner un Future)
        // si ce delai expire. Sans ce try/catch, une simple absence de topic
        // cote broker ferait remonter un 500 sur tout le cycle L1 - contraire a
        // ENF-DIS-04 (une notification qui echoue ne doit jamais bloquer le
        // moteur). La degradation gracieuse doit donc englober l'appel send()
        // lui-meme, pas seulement son whenComplete().
        try {
            String cle = event.demandeId().toString();
            kafkaTemplate.send(TOPIC_PROPOSITION_EMISE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("PropositionEmise publiee - demande={}, rang={}, offset={}",
                                    event.demandeId(), event.rang(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication PropositionEmise (callback async) - demande={}",
                                    event.demandeId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication PropositionEmise (send() bloquant, ex. metadonnees "
                    + "topic indisponibles) - demande={} - cycle L1 non interrompu (ENF-DIS-04)",
                    event.demandeId(), exceptionBlocante);
        }
    }

    public void publierAffectationConfirmee(AffectationConfirmeeEvent event) {
        // Meme piege/meme remede que publierPropositionEmise ci-dessus.
        try {
            String cle = event.missionId().toString();
            kafkaTemplate.send(TOPIC_AFFECTATION_CONFIRMEE, cle, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("AffectationConfirmee publiee - mission={}, offset={}",
                                    event.missionId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Echec publication AffectationConfirmee (callback async) - mission={}",
                                    event.missionId(), ex);
                        }
                    });
        } catch (Exception exceptionBlocante) {
            log.error("Echec publication AffectationConfirmee (send() bloquant, ex. metadonnees "
                    + "topic indisponibles) - mission={} - cycle L1 non interrompu (ENF-DIS-04)",
                    event.missionId(), exceptionBlocante);
        }
    }
}
