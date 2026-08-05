package com.fretcorridor.trk.messaging;

import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ingestion de PositionBrute (module FLT, Mobile -> TRK, async Kafka).
 * EF-TRK-01/02 : tolerant a la connectivite (auto-offset-reset=earliest,
 * cf application.yml - rattrape les messages publies pendant une eventuelle
 * indisponibilite de service-trk).
 *
 * Idempotence (ENF-SEC-03) : repose sur la contrainte UNIQUE(event_id) en
 * base (cf migration V2), pas sur une verification applicative prealable -
 * un doublon (redelivery Kafka at-least-once, ou re-envoi mode hors ligne
 * cote FLT) leve une exception qu'on attrape et logue en INFO, jamais en
 * ERROR : c'est un comportement attendu du systeme, pas une panne.
 */
@Component
public class PositionBruteListener {

    private static final Logger log = LoggerFactory.getLogger(PositionBruteListener.class);

    private final PositionRepository positionRepository;

    public PositionBruteListener(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @KafkaListener(topics = "position-brute", groupId = "service-trk")
    public void ingerer(PositionBruteEvent event) {
        Position position = new Position(
                event.eventId(),
                event.missionId(),
                event.vehiculeId(),
                event.latitude(),
                event.longitude(),
                event.sourceCapture(),
                event.precisionMetres(),
                event.horodatageCapture(),
                event.horodatageTransmission()
        );

        try {
            positionRepository.save(position);
            log.debug("Position ingeree - mission={}, vehicule={}, eventId={}",
                    event.missionId(), event.vehiculeId(), event.eventId());
        } catch (DataIntegrityViolationException doublon) {
            // Cas attendu, pas une panne : re-envoi (redelivery Kafka ou re-sync
            // hors ligne cote FLT) du meme eventId - deja ingere, on ignore.
            log.info("Position deja ingeree, doublon ignore (idempotence) - eventId={}", event.eventId());
        }
    }
}
