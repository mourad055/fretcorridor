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
import java.util.Set;
import java.util.stream.Collectors;

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
     * INCREMENT 21/08 (audit EF-MAT-05/13) : en plus du poids/essieu, verifie
     * desormais le VOLUME dynamique (somme L x l x H x quantite des lots a
     * bord a chaque etat intermediaire vs volume caisse) et le GABARIT
     * (chaque type de lot rentre dans la caisse, rotation permise) des lors
     * que les donnees dimensionnelles existent (LotDemande.longueurM/
     * largeurM/hauteurM + ProfilCamionDto). Donnees manquantes = verification
     * sautee mais TRACEE explicitement comme non verifiee dans
     * PlanChargement.positionsColis - jamais present comme verifie.
     *
     * @param demandeIdParAffectation affectationId -> demandeId pour les
     *        affectations reellement sequencees dans cette tournee (permet de
     *        retrouver les lots a bord a chaque etat sans requete N+1 depuis
     *        l'entite Affectation)
     * @return true si TOUS les etats intermediaires sont faisables - la
     *         Tournee ne doit etre confirmee que si ce resultat est true.
     */
    @Transactional
    public boolean verifierTournee(Tournee tournee, ProfilCamionDto profilCamion,
                                    Map<java.util.UUID, java.util.UUID> demandeIdParAffectation) {
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

        // Donnees dimensionnelles : caisse (profil) + lots (LotDemande).
        double[] caisse = dimensionsCaisse(profilCamion);
        Map<java.util.UUID, List<LotDemande>> lotsParDemande = demandeIdParAffectation.isEmpty()
                ? Map.of()
                : demandeIdParAffectation.values().stream().distinct()
                        .collect(java.util.stream.Collectors.toMap(id -> id,
                                lotDemandeRepository::findByDemandeId));

        // Volume par affectation (m3), null si une seule dimension manque -
        // dans ce cas la verification volumique est sautee (tracee, jamais
        // devinee). Gabarit : chaque triplet de dimensions distinct doit
        // rentrer dans la caisse (rotation permise).
        Map<java.util.UUID, Double> volumeParAffectation = new java.util.HashMap<>();
        boolean dimensionsCompletes = caisse != null;
        List<double[]> gabaritsLots = new java.util.ArrayList<>();
        for (List<LotDemande> lots : lotsParDemande.values()) {
            for (LotDemande lot : lots) {
                double[] dims = dimensionsLot(lot);
                if (dims == null) {
                    dimensionsCompletes = false;
                } else {
                    volumeParAffectation.merge(lot.getDemandeId(),
                            dims[0] * dims[1] * dims[2] * lot.getQuantite(), Double::sum);
                    gabaritsLots.add(dims);
                }
            }
        }
        boolean gabaritOk = true;
        String motifGabarit = null;
        if (caisse != null && !gabaritsLots.isEmpty()) {
            for (double[] lot : gabaritsLots) {
                if (!rentreDansCaisse(lot, caisse)) {
                    gabaritOk = false;
                    motifGabarit = "Lot " + lot[0] + "x" + lot[1] + "x" + lot[2]
                            + " m ne rentre pas dans la caisse " + caisse[0] + "x" + caisse[1]
                            + "x" + caisse[2] + " m (toutes rotations essayees).";
                    break;
                }
            }
        }

        boolean touteFaisable = true;

        for (EtapeTournee etape : etapes) {
            PlanChargement plan = verifierEtat(tournee, etape, profilCamion, profilIncomplet,
                    caisse, volumeParAffectation, dimensionsCompletes, gabaritOk, motifGabarit);
            planChargementRepository.save(plan);
            if (!plan.isFaisable()) {
                touteFaisable = false;
            }
        }

        log.info("Oracle de chargement - tournee={}, {} etat(s) verifie(s), faisable={}, "
                        + "volume/gabarit verifies={}",
                tournee.getId(), etapes.size(), touteFaisable, dimensionsCompletes);

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
                                         ProfilCamionDto profilCamion, boolean profilIncomplet,
                                         double[] caisse, Map<java.util.UUID, Double> volumeParAffectation,
                                         boolean dimensionsCompletes, boolean gabaritOk, String motifGabarit) {

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

        // INCREMENT 21/08 : volume DYNAMIQUE a bord a cet etat intermediaire -
        // meme logique que la charge (RG-104 applique au volume) : une
        // affectation occupe le volume de son enlevement (incluse) jusqu'a sa
        // livraison (exclue). Rejet si caisse connue et volume depasse.
        double volumeABoardsM3 = 0.0;
        if (dimensionsCompletes && !volumeParAffectation.isEmpty()) {
            Set<java.util.UUID> affectationsABoards = affectationsABoards(etape);
            volumeABoardsM3 = affectationsABoards.stream()
                    .map(volumeParAffectation::get)
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }
        double volumeCaisseM3 = caisse != null ? caisse[0] * caisse[1] * caisse[2] : -1.0;
        boolean depassementVolume = dimensionsCompletes && !volumeParAffectation.isEmpty()
                && volumeABoardsM3 > volumeCaisseM3;

        boolean faisable = !depassementEssieu && !depassementPoidsMax && !depassementVolume && gabaritOk;
        String motifRejet = null;
        if (depassementEssieu) {
            motifRejet = "Charge par essieu (approx. uniforme) " + chargeParEssieuApprox
                    + " kg > max " + chargeMaxParEssieuKg + " kg.";
        } else if (depassementPoidsMax) {
            motifRejet = "Charge totale " + chargeKg + " kg > poids max vehicule.";
        } else if (depassementVolume) {
            motifRejet = "Volume a bord " + Math.round(volumeABoardsM3 * 1e4) / 1e4
                    + " m3 > volume caisse " + Math.round(volumeCaisseM3 * 1e4) / 1e4 + " m3.";
        } else if (!gabaritOk) {
            motifRejet = motifGabarit;
        }

        // Trace dimensionnelle (EF-MAT-13) : ce qui a ete verifie ou non, et
        // pourquoi - jamais un silence qui ressemble a une verification ok.
        Map<String, Object> traceDimensionnelle = new LinkedHashMap<>();
        traceDimensionnelle.put("verificationsDimensionnelles",
                dimensionsCompletes ? "VERIFIEES" : "NON_VERIFIEES_DONNEES_MANQUANTES");
        if (dimensionsCompletes) {
            traceDimensionnelle.put("volumeABoardsM3", Math.round(volumeABoardsM3 * 1e6) / 1e6);
            traceDimensionnelle.put("volumeCaisseM3", Math.round(volumeCaisseM3 * 1e6) / 1e6);
            traceDimensionnelle.put("gabaritOk", gabaritOk);
        }

        return new PlanChargement(tournee.getId(), etape.getId(), chargesParEssieu, traceDimensionnelle,
                faisable, motifRejet, false);
    }

    /**
     * Affectations physiquement a bord a l'etat represente par cette etape :
     * enlevees a un rang <= celui-ci et livrees a un rang strictement
     * superieur (meme semantique dynamique que EtatSolution/ALNS).
     */
    private Set<java.util.UUID> affectationsABoards(EtapeTournee etapeCourante) {
        int rang = etapeCourante.getRang() != null ? etapeCourante.getRang() : 0;
        return etapeCourante.getTournee().getEtapes().stream()
                .filter(e -> e.getTypeEtape() == EtapeTournee.TypeEtape.ENLEVEMENT
                        && e.getRang() != null && e.getRang() <= rang)
                .map(EtapeTournee::getAffectationId)
                .filter(id -> etapeCourante.getTournee().getEtapes().stream()
                        .noneMatch(l -> l.getTypeEtape() == EtapeTournee.TypeEtape.LIVRAISON
                                && id.equals(l.getAffectationId())
                                && l.getRang() != null && rang > l.getRang()))
                .collect(Collectors.toSet());
    }

    /**
     * Dimensions caisse depuis le profil vehicule (S8.11.2), ordonnees
     * [longueur, largeur, hauteur] - null si une seule manque (verification
     * volumique/gabarit sautee mais tracee, cf verifierTournee).
     */
    static double[] dimensionsCaisse(ProfilCamionDto profilCamion) {
        if (profilCamion == null || profilCamion.longueurMetres() == null
                || profilCamion.largeurMetres() == null || profilCamion.hauteurMetres() == null) {
            return null;
        }
        return new double[]{profilCamion.longueurMetres(), profilCamion.largeurMetres(),
                profilCamion.hauteurMetres()};
    }

    /**
     * Dimensions d'un lot (L x l x H x quantite cote volume), null si une
     * seule manque - meme traitement que la caisse : saut trace, pas de
     * supposition.
     */
    static double[] dimensionsLot(LotDemande lot) {
        if (lot.getLongueurM() == null || lot.getLargeurM() == null || lot.getHauteurM() == null) {
            return null;
        }
        return new double[]{lot.getLongueurM(), lot.getLargeurM(), lot.getHauteurM()};
    }

    /**
     * Gabarit : un colis rentre dans la caisse si ses trois dimensions
     * rentrent apres PERMUTATION (un colis LxlxH peut etre pose dans les 6
     * orientations) - test exact par tri des deux triplets : colis[i] <=
     * caisse[i] pour i trie decroissant couvre toutes les rotations.
     */
    static boolean rentreDansCaisse(double[] colis, double[] caisse) {
        double[] c = colis.clone();
        double[] k = caisse.clone();
        java.util.Arrays.sort(c);
        java.util.Arrays.sort(k);
        for (int i = 0; i < 3; i++) {
            if (c[i] > k[i]) {
                return false;
            }
        }
        return true;
    }
}
