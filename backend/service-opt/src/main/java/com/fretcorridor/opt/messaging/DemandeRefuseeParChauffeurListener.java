package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.AffectationConfirmationService;
import com.fretcorridor.opt.domain.DemandeEnAttente;
import com.fretcorridor.opt.domain.DemandeEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diffusion-course (plan de reorientation post-demo, partie Chauffeur point 2) :
 * un chauffeur refuse explicitement une proposition qui lui a ete diffusee.
 *
 * Deux effets, atomiquement :
 *   1. Cette Affectation precise est marquee EXPIREE (AffectationConfirmationService
 *      .refuser) - le chauffeur refuse ne pourra plus la confirmer, elle disparait
 *      de son cote (coherent avec "la notification disparait chez les autres").
 *   2. Le transporteur qui refuse est ajoute a la liste d'exclusion de la
 *      demande (DemandeEnAttente.transporteursExclus) puis la demande est remise
 *      en file (traitee=false + date_reception=maintenant) pour qu'un prochain
 *      cycle la diffuse a un AUTRE chauffeur compatible - jamais a celui qui
 *      vient de refuser (plan de reorientation partie Chauffeur point 2).
 */
@Component
public class DemandeRefuseeParChauffeurListener {

    private static final Logger log = LoggerFactory.getLogger(DemandeRefuseeParChauffeurListener.class);

    private final AffectationConfirmationService affectationConfirmationService;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;

    public DemandeRefuseeParChauffeurListener(AffectationConfirmationService affectationConfirmationService,
                                               DemandeEnAttenteRepository demandeEnAttenteRepository) {
        this.affectationConfirmationService = affectationConfirmationService;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
    }

    @Transactional
    @KafkaListener(topics = "demande-refusee-par-chauffeur",
            containerFactory = "demandeRefuseeParChauffeurKafkaListenerContainerFactory")
    public void ingerer(DemandeRefuseeParChauffeurEvent event) {
        log.info("DemandeRefuseeParChauffeur recu - affectation={}, demande={}, capacite={}, transporteur={}",
                event.affectationId(), event.demandeId(), event.capaciteId(), event.transporteurId());

        // 1. Expire cette Affectation (le refuse ne peut plus la confirmer).
        affectationConfirmationService.refuser(event.affectationId());

        // 2. Ajoute le transporteur a la liste d'exclusion de la demande, puis
        //    remet la demande en file pour un prochain cycle, sauf si elle a
        //    deja ete annulee/absente.
        demandeEnAttenteRepository.findByDemandeId(event.demandeId()).ifPresentOrElse(
                demande -> {
                    demande.exclureTransporteur(event.transporteurId());
                    demande.remettreEnFile();
                },
                () -> log.warn("Demande refusee mais introuvable en file - demande={} "
                        + "(deja annulee ou jamais enregistree), re-matching ignore.",
                        event.demandeId()));
    }
}
