package com.fretcorridor.mat.domain;

import com.fretcorridor.mat.web.dto.CoutLotResponse;
import com.fretcorridor.mat.web.dto.CoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Coeur du module MAT (EF-MAT-04) : calcule le cout composite multi-critere
 * d'une capacite face a une demande, a partir des poids actuellement actifs
 * en base (jamais codes en dur - anti-patron CDC S12.4).
 *
 * LIMITATION CONNUE (V0, a revisiter en Phase 2) : les valeurs de critere
 * recues en entree sont supposees deja normalisees sur une echelle comparable
 * (0..1) par l'appelant (service-opt). Ce service ne normalise aucune echelle
 * lui-meme (km vs heures vs score de fiabilite) - documente ici plutot que
 * cache, pour que ca ne soit pas une surprise quand les criteres reels
 * (retour a vide, essieu, etc.) arriveront.
 *
 * Mode degrade (ENF-DIS-04, EF-MAT-12) : si aucun modele de ponderation n'est
 * actif en base, le service NE PLANTE PAS - il retombe sur une ponderation
 * egale entre les criteres fournis et signale explicitement modeDegrade=true,
 * a la fois dans la reponse et dans chaque CycleMatching persiste, plutot
 * que de bloquer OPT.
 */
@Service
public class CoutCompositeService {

    private static final Logger log = LoggerFactory.getLogger(CoutCompositeService.class);

    private final ModelePonderationRepository modelePonderationRepository;
    private final PonderationCritereRepository ponderationCritereRepository;
    private final CycleMatchingRepository cycleMatchingRepository;

    public CoutCompositeService(ModelePonderationRepository modelePonderationRepository,
                                 PonderationCritereRepository ponderationCritereRepository,
                                 CycleMatchingRepository cycleMatchingRepository) {
        this.modelePonderationRepository = modelePonderationRepository;
        this.ponderationCritereRepository = ponderationCritereRepository;
        this.cycleMatchingRepository = cycleMatchingRepository;
    }

    /**
     * Calcule le cout de chaque candidat du lot face a la meme demande, avec
     * UNE SEULE version de modele de ponderation pour tout le lot - jamais un
     * melange de versions entre deux candidats compares l'un a l'autre au sein
     * du meme cycle. Persiste un CycleMatching par candidat (EF-MAT-11).
     *
     * @Transactional : soit tous les CycleMatching du lot sont persistes, soit
     * aucun - evite un etat partiel illisible si une erreur survient en cours
     * de lot (ex. contrainte base violee sur le 50e candidat sur 100).
     */
    @Transactional
    public CoutLotResponse calculerCoutsLot(UUID demandeId, UUID axeId, List<CandidatCout> candidats) {

        Optional<ModelePonderation> modeleActifOpt = resoudreModele(axeId);
        boolean modeDegrade = modeleActifOpt.isEmpty();

        Map<String, BigDecimal> poidsParCritere;
        UUID modeleId;
        Integer version;

        if (modeleActifOpt.isPresent()) {
            ModelePonderation modele = modeleActifOpt.get();
            modeleId = modele.getId();
            version = modele.getVersion();
            poidsParCritere = ponderationCritereRepository.findByModeleId(modeleId).stream()
                    .collect(Collectors.toMap(PonderationCritere::getCodeCritere, PonderationCritere::getPoids));
        } else {
            log.warn("Aucun modele de ponderation actif en base (mat.modele_ponderation) - "
                    + "mode degrade active, ponderation egale appliquee sur les criteres fournis.");
            modeleId = null;
            version = null;
            poidsParCritere = Map.of();
        }

        List<CycleMatching> cyclesAPersister = new ArrayList<>(candidats.size());
        for (CandidatCout candidat : candidats) {
            Map<String, Double> detail = new LinkedHashMap<>();
            BigDecimal cout = calculerCoutCandidat(candidat.valeursCriteres(), poidsParCritere, detail);

            cyclesAPersister.add(new CycleMatching(
                    candidat.capaciteId(), demandeId, modeleId, cout, detail, modeDegrade));
        }

        // saveAll conserve l'ordre d'entree (comportement standard JpaRepository) :
        // on peut donc rezipper avec la liste de candidats sans requete supplementaire.
        List<CycleMatching> cyclesPersistes = cycleMatchingRepository.saveAll(cyclesAPersister);

        List<CoutResponse> resultats = new ArrayList<>(cyclesPersistes.size());
        for (CycleMatching cycle : cyclesPersistes) {
            resultats.add(new CoutResponse(cycle.getCapaciteId(), cycle.getId(), cycle.getCoutTotal()));
        }

        return new CoutLotResponse(demandeId, version, modeDegrade, resultats);
    }

    /**
     * Somme ponderee des criteres fournis, en ne retenant que ceux presents a
     * la fois dans l'entree ET dans le modele actif (un critere fourni sans
     * poids connu est ignore dans le cout, mais reste absent du detail persiste
     * - donc la decision reste lisible : on voit exactement ce qui a compte).
     * Si poidsParCritere est vide (mode degrade), bascule sur une ponderation
     * egale entre tous les criteres fournis par l'appelant.
     */
    private BigDecimal calculerCoutCandidat(Map<String, Double> valeursCriteres,
                                             Map<String, BigDecimal> poidsParCritere,
                                             Map<String, Double> detailSortie) {
        BigDecimal total = BigDecimal.ZERO;

        if (!poidsParCritere.isEmpty()) {
            for (Map.Entry<String, Double> entree : valeursCriteres.entrySet()) {
                BigDecimal poids = poidsParCritere.get(entree.getKey());
                if (poids == null) {
                    continue; // critere hors modele actif : ignore, pas dans le perimetre pondere
                }
                BigDecimal contribution = poids.multiply(BigDecimal.valueOf(entree.getValue()));
                detailSortie.put(entree.getKey(), contribution.doubleValue());
                total = total.add(contribution);
            }
        } else {
            // Mode degrade : ponderation egale sur tous les criteres fournis par l'appelant.
            BigDecimal poidsEgal = BigDecimal.ONE.divide(
                    BigDecimal.valueOf(valeursCriteres.size()), 6, RoundingMode.HALF_UP);
            for (Map.Entry<String, Double> entree : valeursCriteres.entrySet()) {
                BigDecimal contribution = poidsEgal.multiply(BigDecimal.valueOf(entree.getValue()));
                detailSortie.put(entree.getKey(), contribution.doubleValue());
                total = total.add(contribution);
            }
        }

        return total.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Modele specifique a l'axe en priorite, sinon repli sur le modele par
     * defaut (RG-106). Meme patron que
     * TarificationL4Service.resoudreBareme cote service-opt.
     */
    private Optional<ModelePonderation> resoudreModele(UUID axeId) {
        if (axeId != null) {
            Optional<ModelePonderation> specifique = modelePonderationRepository.findFirstByAxeIdAndActifTrue(axeId);
            if (specifique.isPresent()) {
                return specifique;
            }
        }
        return modelePonderationRepository.findFirstByAxeIdIsNullAndActifTrue();
    }
}
