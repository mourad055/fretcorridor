package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.DemandeEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme DemandeAnnulee (service-mkt, Mobile) - retire la demande de la
 * file d'attente de matching (opt.demande_en_attente) avant qu'un cycle ne
 * gaspille une capacite reelle sur une demande deja annulee cote chargeur.
 *
 * BUG CORRIGE (retour utilisateur direct, 22 aout) : sans ce listener, une
 * demande annulee restait "en attente" cote service-opt et etait toujours
 * matchable normalement.
 */
@Component
public class DemandeAnnuleeListener {

    private static final Logger log = LoggerFactory.getLogger(DemandeAnnuleeListener.class);

    private final DemandeEnAttenteRepository repository;

    public DemandeAnnuleeListener(DemandeEnAttenteRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "demande-annulee", containerFactory = "demandeAnnuleeKafkaListenerContainerFactory")
    public void ingerer(DemandeAnnuleeEvent event) {
        var enAttente = repository.findByDemandeIdAndTraiteeFalse(event.demandeId());
        if (enAttente.isEmpty()) {
            log.debug("DemandeAnnulee ignoree - demande={} deja traitee ou jamais en attente (rien a retirer)",
                    event.demandeId());
            return;
        }
        repository.deleteAll(enAttente);
        log.info("Demande retiree de la file d'attente suite a annulation - demande={}", event.demandeId());
    }
}
