package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.AffectationConfirmationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Diffusion-course (plan de reorientation post-demo) : un chauffeur accepte
 * une des propositions PROPOSEE qui lui ont ete diffusees par
 * AffectationL1Service. Premier arrive gagne - la resolution reelle se fait
 * dans AffectationConfirmationService.confirmer (UPDATE conditionnel
 * atomique confirmerSiProposee), jamais en supposant que le premier
 * evenement recu ici est forcement le premier envoye (ordre Kafka non
 * garanti entre partitions).
 *
 * Idempotence : si un autre chauffeur a deja gagne la course avant celui-ci
 * (ou si cette Affectation n'existe plus a l'etat PROPOSEE),
 * AffectationConfirmationService.confirmer retourne 0 ligne affectee et
 * revient sans erreur - comportement attendu de la diffusion-course pour
 * tout candidat qui n'a pas ete le premier (la notification disparait chez
 * lui, plan de reorientation "si l'un accepte, la notification disparait
 * chez les autres").
 *
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile).
 */
@Component
public class DemandeAccepteeListener {

    private static final Logger log = LoggerFactory.getLogger(DemandeAccepteeListener.class);

    private final AffectationConfirmationService affectationConfirmationService;

    public DemandeAccepteeListener(AffectationConfirmationService affectationConfirmationService) {
        this.affectationConfirmationService = affectationConfirmationService;
    }

    @KafkaListener(topics = "demande-acceptee", containerFactory = "demandeAccepteeKafkaListenerContainerFactory")
    public void ingerer(DemandeAccepteeEvent event) {
        log.info("DemandeAcceptee recu - affectation={}, demande={}, transporteur={}",
                event.affectationId(), event.demandeId(), event.transporteurId());
        affectationConfirmationService.confirmer(event.affectationId(), event.transporteurId());
    }
}
