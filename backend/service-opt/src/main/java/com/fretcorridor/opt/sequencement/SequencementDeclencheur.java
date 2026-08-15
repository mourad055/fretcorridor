package com.fretcorridor.opt.sequencement;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fretcorridor.opt.sequencement.alns.AlnsSolver;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Declencheur du sequencement L2 (Sprint 11, CDC S8.6) - meme principe que
 * MatchingCycleService pour L1 : cycle periodique, jamais un declenchement
 * immediat par affectation individuelle (coherent avec "par cycles a
 * fenetre", meme si EF-MAT-01 vise formellement le L1, l'esprit
 * "regrouper avant d'agir" s'applique aussi ici pour permettre a l'ALNS de
 * consolider plusieurs Affectation a la fois plutot que d'en sequencer une
 * seule a chaque tour).
 *
 * Repere les Affectation confirmees (L1) qui n'appartiennent encore a
 * aucune Tournee, les regroupe par capacite, et ne declenche l'ALNS que
 * pour les capacites ayant au moins 2 Affectation non sequencees - une
 * seule affectation seule n'a rien a consolider (une Tournee a une seule
 * etape n'a pas de sens fonctionnel pour ce sprint, cf CDC S8.6 : la
 * consolidation LTL suppose plusieurs demandes).
 */
@Service
public class SequencementDeclencheur {

    private static final Logger log = LoggerFactory.getLogger(SequencementDeclencheur.class);

    private final AffectationRepository affectationRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;
    private final TourneeRepository tourneeRepository;
    private final AlnsSolver alnsSolver;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;

    public SequencementDeclencheur(AffectationRepository affectationRepository,
                                    EtapeTourneeRepository etapeTourneeRepository,
                                    TourneeRepository tourneeRepository,
                                    AlnsSolver alnsSolver,
                                    CapaciteEnAttenteRepository capaciteEnAttenteRepository) {
        this.affectationRepository = affectationRepository;
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.tourneeRepository = tourneeRepository;
        this.alnsSolver = alnsSolver;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
    }

    @Scheduled(fixedDelayString = "${spring.sequencement.cycle-interval-ms:30000}")
    public void executerCycle() {
        List<Affectation> affectationsNonSequencees = affectationRepository.findNonEncoreSequencees();

        if (affectationsNonSequencees.isEmpty()) {
            log.debug("Aucune affectation en attente de sequencement ce tour.");
            return;
        }

        Map<UUID, List<Affectation>> parCapacite = affectationsNonSequencees.stream()
                .collect(Collectors.groupingBy(Affectation::getCapaciteId));

        for (Map.Entry<UUID, List<Affectation>> entree : parCapacite.entrySet()) {
            UUID capaciteId = entree.getKey();
            List<Affectation> affectationsCapacite = entree.getValue();

            if (affectationsCapacite.size() < 2) {
                // Une seule affectation sur cette capacite : rien a consolider
                // ce tour, reste en attente qu'une deuxieme arrive.
                continue;
            }

            log.info("Sequencement L2 declenche - capacite={}, {} affectation(s) a consolider",
                    capaciteId, affectationsCapacite.size());

            // EF-MAT-07 (CDC S8.6.1 point 3, capacite dynamique) : capacite
            // reelle lue depuis la CapaciteEnAttente correspondante (deja
            // traitee=true a ce stade par MatchingCycleService, cf
            // CapaciteEnAttenteRepository.findFirstByCapaciteIdOrderByDateReceptionDesc).
            // Optional vide en theorie impossible (une Affectation n'existe
            // que parce qu'une CapaciteEnAttente a ete consommee par L1) -
            // gere explicitement quand meme : null reste le comportement
            // permissif documente (EtatSolution : pas de borne appliquee),
            // jamais une exception qui romprait le cycle pour les autres
            // capacites du meme tour (ENF-DIS-04).
            java.math.BigDecimal capaciteMaxKg = capaciteEnAttenteRepository
                    .findFirstByCapaciteIdOrderByDateReceptionDesc(capaciteId)
                    .map(CapaciteEnAttente::getCapaciteResiduelleKg)
                    .orElseGet(() -> {
                        log.warn("Aucune CapaciteEnAttente retrouvee pour capaciteId={} - "
                                + "capacite dynamique non bornee ce cycle (cas theoriquement "
                                + "impossible, a investiguer si observe en pratique).", capaciteId);
                        return null;
                    });

            AlnsSolver.ResultatSequencement resultat = alnsSolver.resoudre(
                    affectationsCapacite, capaciteMaxKg, Map.of());

            if (resultat.affectationsInserees().isEmpty()) {
                log.debug("Aucune affectation inseree pour la capacite {} ce tour.", capaciteId);
                continue;
            }

            com.fretcorridor.opt.domain.Affectation premiere = resultat.affectationsInserees().get(0);
            com.fretcorridor.opt.sequencement.Tournee tournee =
                    new com.fretcorridor.opt.sequencement.Tournee(capaciteId, premiere.getAxeId());
            tourneeRepository.save(tournee);

            var sequence = resultat.solutionFinale().getSequence();
            for (int rang = 0; rang < sequence.size(); rang++) {
                var position = sequence.get(rang);
                var typeEtape = position.type() == com.fretcorridor.opt.sequencement.alns.EtatSolution.TypeArret.ENLEVEMENT
                        ? com.fretcorridor.opt.sequencement.EtapeTournee.TypeEtape.ENLEVEMENT
                        : com.fretcorridor.opt.sequencement.EtapeTournee.TypeEtape.LIVRAISON;
                etapeTourneeRepository.save(new com.fretcorridor.opt.sequencement.EtapeTournee(
                        tournee, position.affectationId(), rang, typeEtape, position.chargeApres()));
            }
            tournee.confirmer();
            tourneeRepository.save(tournee);

            log.info("Tournee {} confirmee - capacite={}, {} etape(s), {} affectation(s) non inseree(s)",
                    tournee.getId(), capaciteId, sequence.size(), resultat.affectationsNonInserees().size());
        }
    }
}
