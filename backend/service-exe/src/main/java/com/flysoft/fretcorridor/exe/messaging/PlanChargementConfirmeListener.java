package com.flysoft.fretcorridor.exe.messaging;

import com.flysoft.fretcorridor.exe.entity.PlanChargementEtape;
import com.flysoft.fretcorridor.exe.repository.PlanChargementEtapeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * S16/EF-MAT-13 (audit de suivi, 23 août) : consomme PlanChargeConfirme
 * (service-opt, Moteur) pour l'écran "plan de chargement" (Chauffeur) -
 * jusqu'ici l'événement était publié (SequencementDeclencheur) mais jamais
 * consommé par personne (canal mort), l'écran mobile restait un mock.
 *
 * Chaque save() reste sa propre transaction, même raisonnement que
 * TourneeConstitueeListener/CapaciteDeclareeListener : un doublon sur un
 * état ne doit jamais empêcher l'ingestion des états suivants du même
 * événement (rejeu Kafka, contrainte unique tournee_id+rang idempotente).
 */
@Component
public class PlanChargementConfirmeListener {

    private static final Logger log = LoggerFactory.getLogger(PlanChargementConfirmeListener.class);

    private final PlanChargementEtapeRepository repository;

    public PlanChargementConfirmeListener(PlanChargementEtapeRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "plan-chargement-confirme",
            containerFactory = "planChargementConfirmeKafkaListenerContainerFactory")
    public void ingerer(PlanChargementConfirmeEvent event) {
        for (EtatChargementDto etat : event.etats()) {
            enregistrerEtat(event, etat);
        }
        log.debug("PlanChargementConfirme ingéré - tournee={}, {} état(s)",
                event.tourneeId(), event.etats().size());
    }

    private void enregistrerEtat(PlanChargementConfirmeEvent event, EtatChargementDto etat) {
        try {
            repository.save(PlanChargementEtape.builder()
                    .tourneeId(event.tourneeId())
                    .rang(etat.rang())
                    .chargesParEssieu(etat.chargesParEssieu())
                    .dateGeneration(LocalDateTime.ofInstant(event.dateGeneration(), ZoneId.systemDefault()))
                    .build());
        } catch (DataIntegrityViolationException doublon) {
            log.debug("PlanChargementEtape déjà ingéré, doublon ignoré - tournee={}, rang={}",
                    event.tourneeId(), etat.rang());
        }
    }
}
