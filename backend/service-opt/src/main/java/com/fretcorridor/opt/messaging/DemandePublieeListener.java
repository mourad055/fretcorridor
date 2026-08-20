package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.DemandeEnAttente;
import com.fretcorridor.opt.domain.DemandeEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consomme DemandePubliee (service-mkt, Mobile) - meme principe que CapaciteDeclareeListener. */
@Component
public class DemandePublieeListener {

    private static final Logger log = LoggerFactory.getLogger(DemandePublieeListener.class);

    private final DemandeEnAttenteRepository repository;

    public DemandePublieeListener(DemandeEnAttenteRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "demande-publiee", containerFactory = "demandePublieeKafkaListenerContainerFactory")
    public void ingerer(DemandePublieeEvent event) {
        try {
            repository.save(new DemandeEnAttente(
                    event.demandeId(), event.axeId(), event.eventId(), event.valeursCriteres(),
                    event.origine(), event.destination(), event.poidsTaxableKg(),
                    event.fenetreDebut(), event.fenetreFin()));
            log.debug("Demande mise en attente - demande={}, axe={}, eventId={}",
                    event.demandeId(), event.axeId(), event.eventId());
        } catch (DataIntegrityViolationException doublon) {
            log.info("DemandePubliee deja ingeree, doublon ignore - eventId={}", event.eventId());
        }
    }
}
