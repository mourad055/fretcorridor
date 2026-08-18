package com.fretcorridor.opt.sequencement;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.PropositionRetourAVideEvent;
import com.fretcorridor.opt.sequencement.alns.AlnsSolver;
import com.fretcorridor.opt.sequencement.alns.EtatSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EF-MAT-09 (Sprint 12, CDC S8.6) : replanifie une tournee EN COURS en
 * figeant l'execute - jamais de recalcul retroactif d'une etape EXECUTEE.
 *
 * Declencheur : meme cycle periodique que le sequencement initial
 * (SequencementDeclencheur), pas un nouvel evenement.
 *
 * Distinction cle (correctif Sprint 12) entre deux categories d'affectations
 * residuelles :
 *  - "en transit" : enlevement deja EXECUTEE, livraison encore PLANIFIEE -
 *    la marchandise est physiquement a bord. Sa livraison reste FIGEE
 *    (ni supprimee ni recalculee) - reinserer un couple enlevement+livraison
 *    via l'ALNS pour une marchandise deja chargee serait faux (creerait un
 *    second enlevement fictif). Son poids alimente chargeInitiale.
 *  - "non demarree" : ni enlevement ni livraison executes - reinseree
 *    normalement (couple complet) par l'ALNS, comme une nouvelle affectation.
 */
@Service
public class ReplanificationService {

    private static final Logger log = LoggerFactory.getLogger(ReplanificationService.class);

    private final TourneeRepository tourneeRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;
    private final AffectationRepository affectationRepository;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final AlnsSolver alnsSolver;
    private final OptEventPublisher eventPublisher;

    public ReplanificationService(TourneeRepository tourneeRepository,
                                   EtapeTourneeRepository etapeTourneeRepository,
                                   AffectationRepository affectationRepository,
                                   CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                   AlnsSolver alnsSolver,
                                   OptEventPublisher eventPublisher) {
        this.tourneeRepository = tourneeRepository;
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.affectationRepository = affectationRepository;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.alnsSolver = alnsSolver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @param tourneeExistante tournee CONFIRMEE ou EN_EXECUTION deja
     *                         trouvee par l'appelant
     * @param nouvellesAffectations Affectation(s) fraichement confirmees
     *                              (L1) sur la meme capacite, pas encore
     *                              sequencees
     */
    @Transactional
    public void replanifierResiduel(Tournee tourneeExistante, List<Affectation> nouvellesAffectations) {
        UUID capaciteId = tourneeExistante.getCapaciteId();

        List<EtapeTournee> toutesEtapes = etapeTourneeRepository
                .findByTourneeIdOrderByRangAsc(tourneeExistante.getId());

        List<EtapeTournee> etapesExecutees = toutesEtapes.stream()
                .filter(e -> e.getEtat() == EtapeTournee.Etat.EXECUTEE)
                .toList();
        List<EtapeTournee> etapesPlanifiees = toutesEtapes.stream()
                .filter(e -> e.getEtat() == EtapeTournee.Etat.PLANIFIEE)
                .toList();

        // Affectations "en transit" : enlevement EXECUTEE, livraison encore
        // PLANIFIEE - la marchandise est a bord. Identifiees par les
        // enlevements executes qui n'ont pas de livraison executee associee.
        Set<UUID> affectationIdsEnTransit = etapesExecutees.stream()
                .filter(e -> e.getTypeEtape() == EtapeTournee.TypeEtape.ENLEVEMENT)
                .map(EtapeTournee::getAffectationId)
                .filter(id -> etapesExecutees.stream().noneMatch(
                        e -> e.getAffectationId().equals(id) && e.getTypeEtape() == EtapeTournee.TypeEtape.LIVRAISON))
                .collect(Collectors.toSet());

        // Livraisons figees : celles des affectations en transit - ni
        // supprimees ni recalculees, leur position reste telle quelle.
        List<EtapeTournee> livraisonsEnTransitFigees = etapesPlanifiees.stream()
                .filter(e -> affectationIdsEnTransit.contains(e.getAffectationId()))
                .toList();

        // Ce qui reste vraiment a reordonnancer par l'ALNS : les etapes
        // PLANIFIEE d'affectations NON en transit (jamais demarrees).
        List<EtapeTournee> etapesAReordonnancer = etapesPlanifiees.stream()
                .filter(e -> !affectationIdsEnTransit.contains(e.getAffectationId()))
                .toList();

        BigDecimal chargeDejaABord = affectationIdsEnTransit.stream()
                .map(id -> affectationRepository.findById(id)
                        .map(Affectation::getPoidsTaxableKg)
                        .orElse(BigDecimal.ZERO))
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!affectationIdsEnTransit.isEmpty()) {
            log.info("Replanification capacite={} : {} affectation(s) en transit (marchandise a bord), "
                    + "chargeDejaABord={} kg - livraison(s) figee(s), non recalculee(s).",
                    capaciteId, affectationIdsEnTransit.size(), chargeDejaABord);
        }

        List<UUID> affectationIdsNonDemarrees = etapesAReordonnancer.stream()
                .map(EtapeTournee::getAffectationId)
                .distinct()
                .toList();
        List<Affectation> affectationsNonDemarrees = affectationIdsNonDemarrees.isEmpty()
                ? List.of()
                : affectationRepository.findAllById(affectationIdsNonDemarrees);

        List<Affectation> affectationsAReplanifier = java.util.stream.Stream
                .concat(affectationsNonDemarrees.stream(), nouvellesAffectations.stream())
                .toList();

        if (affectationsAReplanifier.isEmpty() && affectationIdsEnTransit.isEmpty()) {
            log.debug("Replanification declenchee mais rien a reordonnancer - capacite={}, tournee={}",
                    capaciteId, tourneeExistante.getId());
            return;
        }

        int rangDepart = toutesEtapes.stream()
                .filter(e -> !etapesAReordonnancer.contains(e))
                .mapToInt(EtapeTournee::getRang)
                .max()
                .orElse(-1) + 1;

        BigDecimal capaciteMaxKg = capaciteEnAttenteRepository
                .findFirstByCapaciteIdOrderByDateReceptionDesc(capaciteId)
                .map(CapaciteEnAttente::getCapaciteResiduelleKg)
                .orElse(null);

        AlnsSolver.ResultatSequencement resultat = alnsSolver.resoudre(
                affectationsAReplanifier, capaciteMaxKg, chargeDejaABord, Map.of());

        // Seules les etapes vraiment reordonnancees sont supprimees - jamais
        // les livraisons en transit figees.
        //
        // flush() OBLIGATOIRE ici : Hibernate ordonne par defaut les
        // INSERT avant les DELETE dans une meme transaction, quel que soit
        // l'ordre d'appel en code - sans ce flush, un nouveau save() plus
        // bas peut s'executer AVANT que ce delete soit reellement applique
        // en base, et violer uq_etape_tournee_rang si le nouveau rang
        // coincide avec une ancienne etape pas encore supprimee (bug
        // observe en boucle toutes les 30s, cf SequencementDeclencheur).
        if (!etapesAReordonnancer.isEmpty()) {
            etapeTourneeRepository.deleteAll(etapesAReordonnancer);
            etapeTourneeRepository.flush();
        }

        var sequence = resultat.solutionFinale().getSequence();
        for (int i = 0; i < sequence.size(); i++) {
            var position = sequence.get(i);
            EtapeTournee.TypeEtape typeEtape = position.type() == EtatSolution.TypeArret.ENLEVEMENT
                    ? EtapeTournee.TypeEtape.ENLEVEMENT
                    : EtapeTournee.TypeEtape.LIVRAISON;
            etapeTourneeRepository.save(new EtapeTournee(
                    tourneeExistante, position.affectationId(), rangDepart + i, typeEtape, position.chargeApres()));
        }

        log.info("Tournee {} replanifiee (EF-MAT-09) - capacite={}, {} etape(s) executee(s) figee(s), "
                        + "{} livraison(s) en transit figee(s), {} nouvelle(s) etape(s) sequencee(s), "
                        + "{} affectation(s) non inseree(s)",
                tourneeExistante.getId(), capaciteId, etapesExecutees.size(),
                livraisonsEnTransitFigees.size(), sequence.size(), resultat.affectationsNonInserees().size());
    }

    /**
     * EF-MAT-08 / RG-058 (Sprint 12) : propose un retour a vide une fois la
     * Tournee TERMINEE - appele exactement une fois par EtapeExecuteeListener.
     */
    @Transactional
    public void proposerRetourAVide(UUID tourneeId) {
        List<EtapeTournee> etapes = etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tourneeId);

        EtapeTournee derniereLivraison = etapes.stream()
                .filter(e -> e.getTypeEtape() == EtapeTournee.TypeEtape.LIVRAISON)
                .max((a, b) -> Integer.compare(a.getRang(), b.getRang()))
                .orElse(null);

        if (derniereLivraison == null) {
            log.warn("Tournee {} terminee sans aucune etape LIVRAISON - retour a vide "
                    + "non propose (cas theoriquement impossible, a investiguer).", tourneeId);
            return;
        }

        Affectation derniereAffectation = affectationRepository.findById(derniereLivraison.getAffectationId())
                .orElse(null);
        if (derniereAffectation == null) {
            log.warn("Tournee {} : Affectation {} introuvable pour la derniere livraison - "
                    + "retour a vide non propose.", tourneeId, derniereLivraison.getAffectationId());
            return;
        }

        Tournee tournee = derniereLivraison.getTournee();

        PropositionRetourAVideEvent event = new PropositionRetourAVideEvent(
                UUID.randomUUID(),
                tourneeId,
                null,
                tournee.getCapaciteId(),
                tournee.getAxeId(),
                derniereAffectation.getDestinationLatitude(),
                derniereAffectation.getDestinationLongitude(),
                java.time.Instant.now());

        eventPublisher.publierPropositionRetourAVide(event);
        log.info("Retour a vide propose (EF-MAT-08) - tournee={}, capacite={}, point depart=({}, {})",
                tourneeId, tournee.getCapaciteId(),
                derniereAffectation.getDestinationLatitude(), derniereAffectation.getDestinationLongitude());
    }

    /**
     * EF-MAT-08 / RG-058 - variante pour une Affectation FTL simple, jamais
     * sequencee en Tournee (cf SequencementDeclencheur).
     */
    @Transactional
    public void proposerRetourAVide(Affectation affectationTerminee) {
        PropositionRetourAVideEvent event = new PropositionRetourAVideEvent(
                UUID.randomUUID(),
                null,
                affectationTerminee.getId(),
                affectationTerminee.getCapaciteId(),
                affectationTerminee.getAxeId(),
                affectationTerminee.getDestinationLatitude(),
                affectationTerminee.getDestinationLongitude(),
                java.time.Instant.now());

        eventPublisher.publierPropositionRetourAVide(event);
        log.info("Retour a vide propose (EF-MAT-08, affectation FTL simple) - affectation={}, capacite={}, "
                        + "point depart=({}, {})",
                affectationTerminee.getId(), affectationTerminee.getCapaciteId(),
                affectationTerminee.getDestinationLatitude(), affectationTerminee.getDestinationLongitude());
    }
}
