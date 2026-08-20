package com.flysoft.fretcorridor.mkt.messaging;

import com.flysoft.fretcorridor.mkt.entity.Proposition;
import com.flysoft.fretcorridor.mkt.repository.PropositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Meme pattern que les listeners cote Moteur (CapaciteDeclareeListener, etc.) :
// idempotence par eventId unique, doublon ignore plutot que rejete en erreur.
@Component
public class PropositionEmiseListener {

    private static final Logger log = LoggerFactory.getLogger(PropositionEmiseListener.class);

    private final PropositionRepository repository;

    public PropositionEmiseListener(PropositionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "proposition-emise", containerFactory = "propositionEmiseKafkaListenerContainerFactory")
    public void ingerer(PropositionEmiseEvent event) {
        try {
            repository.save(Proposition.builder()
                    .eventId(event.eventId())
                    .demandeId(event.demandeId())
                    .cycleMatchingId(event.cycleMatchingId())
                    .capaciteId(event.capaciteId())
                    .missionId(event.missionId())
                    .axeId(event.axeId())
                    .rang(event.rang())
                    .motifClassement(event.motifClassement())
                    .prixTransport(event.prixTransport())
                    .commissionPlateforme(event.commissionPlateforme())
                    .devise(event.devise() != null ? event.devise() : "XAF")
                    .distanceEstimeeMetres(event.distanceEstimeeMetres())
                    .dureeEstimeeSecondes(event.dureeEstimeeSecondes())
                    .origineNom(event.origineNom())
                    .destinationNom(event.destinationNom())
                    .horodatageEmission(event.horodatageEmission())
                    .build());
            log.info("PropositionEmise recue et persistee - demande={}, rang={}",
                    event.demandeId(), event.rang());
        } catch (DataIntegrityViolationException doublon) {
            log.info("PropositionEmise deja recue, doublon ignore - eventId={}", event.eventId());
        }
    }
}
