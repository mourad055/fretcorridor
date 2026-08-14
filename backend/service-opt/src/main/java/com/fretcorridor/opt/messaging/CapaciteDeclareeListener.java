package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme CapaciteDeclaree (service-cap, Mobile) et met en attente pour le
 * prochain cycle de matching (MatchingCycleService) - jamais de matching
 * immediat, conforme a EF-MAT-01 ("par cycles a fenetre").
 */
@Component
public class CapaciteDeclareeListener {

    private static final Logger log = LoggerFactory.getLogger(CapaciteDeclareeListener.class);

    private final CapaciteEnAttenteRepository repository;

    public CapaciteDeclareeListener(CapaciteEnAttenteRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "capacite-declaree", containerFactory = "capaciteDeclareeKafkaListenerContainerFactory")
    public void ingerer(CapaciteDeclareeEvent event) {
        try {
            repository.save(new CapaciteEnAttente(
                    event.capaciteId(), event.axeId(), event.transporteurId(), event.vehiculeId(),
                    event.eventId(), event.valeursCriteres(),
                    event.position(), event.profilCamion(), event.typeVehicule()));
            log.debug("Capacite mise en attente - capacite={}, axe={}, eventId={}",
                    event.capaciteId(), event.axeId(), event.eventId());
        } catch (DataIntegrityViolationException doublon) {
            log.info("CapaciteDeclaree deja ingeree, doublon ignore - eventId={}", event.eventId());
        }
    }
}
