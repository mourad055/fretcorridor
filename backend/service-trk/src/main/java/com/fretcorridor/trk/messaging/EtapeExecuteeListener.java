package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.domain.ColisRecuperation;
import com.fretcorridor.trk.domain.ColisRecuperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Point 6 du plan de reorientation : "colis recupere = position chauffeur".
 *
 * Consomme EtapeExecutee (service-exe -> Mobile) et, des qu'une etape
 * ENLEVEMENT est executee, enregistre l'instant de recuperation du colis pour
 * la mission. C'est ce signal qui fait basculer le suivi de la "position
 * estimee du colis" (son point d'enlevement) vers la "position GPS temps reel
 * du chauffeur" (consulte via SuiviController).
 *
 * Idempotence : une seule ligne par mission (PK = mission_id). Un doublon
 * Kafka (retry reseau cote EXE) leve une DataIntegrityViolationException que
 * l'on ignore proprement - la recuperation ne doit jamais etre re-instantiee
 * ni ecrasee (l'enlevement execute est un evenement unique, EF-MAT-09).
 */
@Component
public class EtapeExecuteeListener {

    private static final Logger log = LoggerFactory.getLogger(EtapeExecuteeListener.class);

    private final ColisRecuperationRepository colisRecuperationRepository;

    public EtapeExecuteeListener(ColisRecuperationRepository colisRecuperationRepository) {
        this.colisRecuperationRepository = colisRecuperationRepository;
    }

    @KafkaListener(topics = "etape-executee",
            containerFactory = "etapeExecuteeKafkaListenerContainerFactory")
    @Transactional
    public void enregistrerEnlevement(EtapeExecuteeEvent event) {
        if (event.typeEtape() != EtapeExecuteeEvent.TypeEtape.ENLEVEMENT) {
            // Une livraison n'a aucun impact sur l'etat "colis a bord" (il y
            // est deja depuis l'enlevement). Rien a faire - on ne cree jamais
            // de colis_recuperation pour une livraison.
            log.debug("EtapeExecutee LIVRAISON - aucun effet sur colis_recuperation, mission={}",
                    event.missionId());
            return;
        }

        Instant horodatage = event.horodatageExecution() != null
                ? event.horodatageExecution()
                : Instant.now();

        try {
            colisRecuperationRepository.save(new ColisRecuperation(event.missionId(), horodatage));
            log.info("Colis recupere (ENLEVEMENT execute) - mission={}, horodatageEnlevement={}",
                    event.missionId(), horodatage);
        } catch (DataIntegrityViolationException doublon) {
            log.info("Enlevement deja confirme pour la mission {} - doublon Kafka ignore "
                    + "(idempotence par PK mission_id).", event.missionId());
        }
    }
}
