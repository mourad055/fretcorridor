package com.fretcorridor.opt.oracle;

import com.fretcorridor.opt.client.ProfilCamionDto;
import com.fretcorridor.opt.sequencement.EtapeTournee;
import com.fretcorridor.opt.sequencement.Tournee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle de chargement 3D (CDC S8.7, EF-MAT-07/13, Sprint 16) - poste le
 * plus incertain du plan technique ("aucune bibliotheque libre ne le
 * couvre entierement").
 *
 * LIMITATION ASSUMEE ET DOCUMENTEE (README_ORACLE_3D.md S6, point 1) : le
 * contrat colis (Lot/Colis, CDC S13 - gerbabilite/fragilite/positions
 * reelles) n'est pas encore valide avec Mobile. Cette version verifie
 * uniquement :
 *  1) La charge totale a chaque etape (deja garantie par EtatSolution/ALNS,
 *     revalidee ici comme deuxieme ligne de defense independante).
 *  2) Une APPROXIMATION de charge par essieu par repartition UNIFORME du
 *     poids total sur le nombre d'essieux - PAS la vraie repartition
 *     physique (qui depend des positions reelles des colis, inconnues tant
 *     que le contrat colis n'existe pas). Documente explicitement dans
 *     PlanChargement.motifRejet quand cette approximation est utilisee,
 *     jamais present comme un resultat certain.
 *
 * Conforme au flux d'exception E1 du CDC (README S4) : en cas de doute
 * (donnee manquante empechant une conclusion sure), la Tournee n'est
 * JAMAIS confirmee - jamais de resultat degrade optimiste, contrairement
 * a MAT/Valhalla.
 */
@Service
public class OracleChargementService {

    private static final Logger log = LoggerFactory.getLogger(OracleChargementService.class);

    private final PlanChargementRepository planChargementRepository;
    private final LotDemandeRepository lotDemandeRepository;

    public OracleChargementService(PlanChargementRepository planChargementRepository,
                                    LotDemandeRepository lotDemandeRepository) {
        this.planChargementRepository = planChargementRepository;
        this.lotDemandeRepository = lotDemandeRepository;
    }

    /**
     * EF-MKT-10 (CDC) : "parcours marchandises dangereuses (restriction
     * transporteurs, anti-groupage incompatible)". Verification faisable
     * SANS connaitre les positions physiques (contrairement au vrai
     * bin-packing 3D, hors de portee ici) - contrairement a EF-MAT-07, cette
     * regle ne depend que de la LISTE des demandes consolidees, pas de leur
     * arrangement spatial. Regle conservatrice : une tournee consolidant
     * plusieurs demandes est rejetee des qu'UNE SEULE d'entre elles porte un
     * lot avec classeDanger non nul - le referentiel de compatibilite entre
     * classes de danger est hors perimetre Moteur (CDC : "referentiel de
     * classes hors perimetre Moteur"), donc pas de logique plus fine que
     * "jamais de matiere dangereuse en groupage" tant que ce referentiel
     * n'existe pas.
     */
    public boolean groupageCompatibleMatieresDangereuses(java.util.List<java.util.UUID> demandeIds) {
        if (demandeIds.size() <= 1) {
            return true; // FTL simple ou une seule demande - rien a groupager
        }
        return demandeIds.stream()
                .flatMap(id -> lotDemandeRepository.findByDemandeId(id).stream())
                .noneMatch(lot -> lot.getClasseDanger() != null);
    }

    /**
     * Verifie chaque etat intermediaire d'une Tournee (une EtapeTournee =
     * un etat, cf README S3 - granularite confirmee). Persiste un
     * PlanChargement par etape, meme quand faisable=true : trace complete,
     * pas seulement les rejets (coherent avec CycleMatching, qui trace
     * chaque decision, pas seulement les echecs).
     *
     * @return true si TOUS les etats intermediaires sont faisables - la
     *         Tournee ne doit etre confirmee que si ce resultat est true.
     */
    @Transactional
    public boolean verifierTournee(Tournee tournee, ProfilCamionDto profilCamion) {
        List<EtapeTournee> etapes = tournee.getEtapes();

        if (etapes.isEmpty()) {
            log.warn("Tournee {} sans etape - rien a verifier, jamais faisable par defaut (E1).",
                    tournee.getId());
            return false;
        }

        boolean profilIncomplet = profilCamion == null
                || profilCamion.nombreEssieux() == null
                || profilCamion.nombreEssieux() <= 0
                || profilCamion.chargeMaxParEssieuTonnes() == null;

        boolean touteFaisable = true;

        for (EtapeTournee etape : etapes) {
            PlanChargement plan = verifierEtat(tournee, etape, profilCamion, profilIncomplet);
            planChargementRepository.save(plan);
            if (!plan.isFaisable()) {
                touteFaisable = false;
            }
        }

        log.info("Oracle de chargement - tournee={}, {} etat(s) verifie(s), faisable={}",
                tournee.getId(), etapes.size(), touteFaisable);

        return touteFaisable;
    }

    /**
     * EF-MAT-13 : reconstruit la liste ordonnee des etats de chargement
     * d'une Tournee DEJA CONFIRMEE (appele uniquement apres verifierTournee()
     * == true, cf SequencementDeclencheur) - jointure PlanChargement/rang
     * EtapeTournee, aucune des deux entites ne porte l'autre directement.
     */
    public java.util.List<com.fretcorridor.opt.messaging.EtatChargementDto> construireEtatsPourRestitution(Tournee tournee) {
        Map<java.util.UUID, Integer> rangParEtape = tournee.getEtapes().stream()
                .collect(java.util.stream.Collectors.toMap(EtapeTournee::getId, EtapeTournee::getRang));

        return planChargementRepository.findByTourneeIdOrderByDateCreationAsc(tournee.getId()).stream()
                .map(plan -> new com.fretcorridor.opt.messaging.EtatChargementDto(
                        plan.getEtapeTourneeId(),
                        rangParEtape.getOrDefault(plan.getEtapeTourneeId(), -1),
                        plan.getChargesParEssieu()))
                .sorted(java.util.Comparator.comparingInt(com.fretcorridor.opt.messaging.EtatChargementDto::rang))
                .toList();
    }

    private PlanChargement verifierEtat(Tournee tournee, EtapeTournee etape,
                                         ProfilCamionDto profilCamion, boolean profilIncomplet) {

        BigDecimal chargeKg = etape.getChargeApresEtapeKg();

        if (chargeKg == null) {
            // Ne devrait jamais arriver (colonne NOT NULL en base), mais E1
            // impose de ne jamais deviner - rejet explicite plutot qu'une
            // NullPointerException qui bloquerait tout le cycle.
            return new PlanChargement(tournee.getId(), etape.getId(), Map.of(), null,
                    false, "Charge inconnue a cette etape - donnee manquante, oracle ne peut pas conclure.", true);
        }

        if (profilIncomplet) {
            // E1 : profil vehicule incomplet (nombreEssieux/chargeMaxParEssieuTonnes
            // absents, cf ProfilCamionDto javadoc - completude faible attendue
            // sur les donnees ouvertes africaines) - jamais suppose, jamais
            // laisse passer avec un profil invente.
            return new PlanChargement(tournee.getId(), etape.getId(), Map.of(), null,
                    false, "Profil vehicule incomplet (nombreEssieux/chargeMaxParEssieuTonnes manquant) - "
                            + "oracle ne peut pas verifier la charge par essieu.", true);
        }

        // Approximation par repartition uniforme (cf javadoc classe) - PAS
        // la vraie repartition physique.
        BigDecimal chargeMaxParEssieuKg = BigDecimal.valueOf(profilCamion.chargeMaxParEssieuTonnes())
                .multiply(BigDecimal.valueOf(1000));
        BigDecimal chargeParEssieuApprox = chargeKg.divide(
                BigDecimal.valueOf(profilCamion.nombreEssieux()), 2, RoundingMode.HALF_UP);

        Map<String, Object> chargesParEssieu = new LinkedHashMap<>();
        for (int i = 1; i <= profilCamion.nombreEssieux(); i++) {
            chargesParEssieu.put("essieu_" + i, chargeParEssieuApprox);
        }

        boolean depassementEssieu = chargeParEssieuApprox.compareTo(chargeMaxParEssieuKg) > 0;

        boolean depassementPoidsMax = profilCamion.poidsMaxTonnes() != null
                && chargeKg.compareTo(BigDecimal.valueOf(profilCamion.poidsMaxTonnes()).multiply(BigDecimal.valueOf(1000))) > 0;

        boolean faisable = !depassementEssieu && !depassementPoidsMax;
        String motifRejet = null;
        if (depassementEssieu) {
            motifRejet = "Charge par essieu (approx. uniforme) " + chargeParEssieuApprox
                    + " kg > max " + chargeMaxParEssieuKg + " kg.";
        } else if (depassementPoidsMax) {
            motifRejet = "Charge totale " + chargeKg + " kg > poids max vehicule.";
        }

        return new PlanChargement(tournee.getId(), etape.getId(), chargesParEssieu, null,
                faisable, motifRejet, false);
    }
}
