package com.fretcorridor.opt.messaging;

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
 * (EF-MAT-09) - declenche en cascade Tournee.marquerEnExecutionSiNecessaire(),
 * qui elle-meme transitionne vers TERMINEE si c'etait la derniere etape
 * (EF-MAT-08/RG-058, cf Tournee.java).
 *
 * Idempotence NON geree par une table dediee ici (contrairement a
 * CapaciteDeclareeListener/DataIntegrityViolationException) : marquerExecutee()
 * est naturellement idempotent (Etat.EXECUTEE applique deux fois reste
 * Etat.EXECUTEE, aucun effet de bord observable) - pas besoin d'un
 * mecanisme d'exclusion supplementaire.
 */
@Component
public class EtapeExecuteeListener {

    private static final Logger log = LoggerFactory.getLogger(EtapeExecuteeListener.class);

    private final EtapeTourneeRepository etapeTourneeRepository;
    private final ReplanificationService replanificationService;

    public EtapeExecuteeListener(EtapeTourneeRepository etapeTourneeRepository,
                                  ReplanificationService replanificationService) {
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.replanificationService = replanificationService;
    }

    @KafkaListener(topics = "etape-executee", containerFactory = "etapeExecuteeKafkaListenerContainerFactory")
    @Transactional
    public void ingerer(EtapeExecuteeEvent event) {
        EtapeTournee.TypeEtape typeEtape = EtapeTournee.TypeEtape.valueOf(event.typeEtape().name());

        Optional<EtapeTournee> etapeOpt = etapeTourneeRepository
                .findByAffectationIdAndTypeEtape(event.missionId(), typeEtape);

        if (etapeOpt.isEmpty()) {
            // Pas une erreur bloquante (ENF-DIS-04) : l'affectation existe
            // peut-etre mais n'a pas encore ete sequencee en Tournee
            // (Sprint 11 pas encore passe sur ce cycle), ou l'evenement est
            // arrive avant sa contrepartie L1/L2. Loggue pour investigation,
            // jamais rejete en erreur Kafka (pas de retry infini a prevoir).
            log.warn("EtapeExecutee recue pour une mission/type non sequence en Tournee - "
                    + "mission={}, typeEtape={}, ignore.", event.missionId(), event.typeEtape());
            return;
        }

        EtapeTournee etape = etapeOpt.get();
        boolean tourneeVientDeTerminer = etape.marquerExecutee();
        log.info("Etape figee (EF-MAT-09) - mission={}, typeEtape={}", event.missionId(), event.typeEtape());

        // EF-MAT-08/RG-058 : uniquement quand CETTE execution vient de faire
        // passer la tournee a TERMINEE (jamais avant, jamais une deuxieme
        // fois sur un evenement redelivre - marquerExecutee() ne renvoie
        // true qu'une seule fois, au moment exact de la transition).
        if (tourneeVientDeTerminer) {
            replanificationService.proposerRetourAVide(etape.getTournee().getId());
        }
    }
}
