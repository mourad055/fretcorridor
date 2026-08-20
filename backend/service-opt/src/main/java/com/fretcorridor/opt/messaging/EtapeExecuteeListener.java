package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.sequencement.EtapeTournee;
import com.fretcorridor.opt.sequencement.EtapeTourneeRepository;
import com.fretcorridor.opt.sequencement.ReplanificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consomme EtapeExecutee (service-exe, Mobile) et fige l'etape correspondante
 * (EF-MAT-09). Deux chemins selon que la mission a ete sequencee en Tournee
 * ou non (cf SequencementDeclencheur).
 */
@Component
public class EtapeExecuteeListener {

    private static final Logger log = LoggerFactory.getLogger(EtapeExecuteeListener.class);

    private final EtapeTourneeRepository etapeTourneeRepository;
    private final AffectationRepository affectationRepository;
    private final ReplanificationService replanificationService;

    public EtapeExecuteeListener(EtapeTourneeRepository etapeTourneeRepository,
                                  AffectationRepository affectationRepository,
                                  ReplanificationService replanificationService) {
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.affectationRepository = affectationRepository;
        this.replanificationService = replanificationService;
    }

    @KafkaListener(topics = "etape-executee", containerFactory = "etapeExecuteeKafkaListenerContainerFactory")
    @Transactional
    public void ingerer(EtapeExecuteeEvent event) {
        EtapeTournee.TypeEtape typeEtape = EtapeTournee.TypeEtape.valueOf(event.typeEtape().name());

        Optional<EtapeTournee> etapeOpt = etapeTourneeRepository
                .findByAffectationIdAndTypeEtape(event.missionId(), typeEtape);

        if (etapeOpt.isPresent()) {
            EtapeTournee etape = etapeOpt.get();
            boolean tourneeVientDeTerminer = etape.marquerExecutee();
            log.info("Etape figee (EF-MAT-09) - mission={}, typeEtape={}", event.missionId(), event.typeEtape());

            if (tourneeVientDeTerminer) {
                replanificationService.proposerRetourAVide(etape.getTournee().getId());
            }
            return;
        }

        if (typeEtape != EtapeTournee.TypeEtape.LIVRAISON) {
            log.debug("EtapeExecutee (ENLEVEMENT) recue sans Tournee associee - mission={}, ignore.",
                    event.missionId());
            return;
        }

        Optional<Affectation> affectationOpt = affectationRepository.findById(event.missionId());
        if (affectationOpt.isEmpty()) {
            log.warn("EtapeExecutee (LIVRAISON) recue pour un missionId introuvable (ni Tournee, "
                    + "ni Affectation) - mission={}, ignore.", event.missionId());
            return;
        }

        Affectation affectation = affectationOpt.get();
        boolean vientDetreMarquee = affectation.marquerLivraisonExecuteeSiNecessaire();
        affectationRepository.save(affectation);

        if (vientDetreMarquee) {
            log.info("Livraison figee (EF-MAT-09, affectation FTL simple) - affectation={}", affectation.getId());
            replanificationService.proposerRetourAVide(affectation);
        } else {
            log.debug("EtapeExecutee (LIVRAISON) redelivree pour une affectation deja marquee - "
                    + "affectation={}, ignoree (idempotence, ENF-SEC-03).", affectation.getId());
        }
    }
}
