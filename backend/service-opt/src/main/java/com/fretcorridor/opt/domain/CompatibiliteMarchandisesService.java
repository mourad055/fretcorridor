package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.oracle.LotDemande;
import com.fretcorridor.opt.oracle.LotDemandeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Point 5 du plan de reorientation : matrice d'incompatibilite marchandises,
 * appliquee comme REGLE DE FILTRAGE DURE AVANT L1 (avant meme le calcul de
 * cout MAT) - jamais une penalite (cf plan : "pas une penalite").
 *
 * Deux facettes, toutes deux pilotees par configuration (jamais codees en
 * dur, coherent avec l'anti-patron evite partout ailleurs dans le moteur) :
 *
 *  1. Anti-groupage des matieres dangereuses (extension du pattern EF-MKT-10
 *     existant, cf OracleChargementService) : deux demandes portant chacune
 *     au moins un lot avec classeDanger != null ne peuvent pas cohabiter sur
 *     la meme capacite de maniere concurrente. Pilote par la propriete
 *     fretcorridor.opt.compatibilite-marchandises.anti-groupage-matieres-dangereuses.
 *     Cote OPT, le filtre PRE-L1 couvre le cas "demande candidate vs
 *     demandes deja confirmees sur la capacite" (une capacite ne doit pas
 *     recevoir une nouvelle demande incompatible avec ce qu'elle transporte
 *     deja) ; le groupage intra-tournee reste verifie par l'oracle existant.
 *
 *  2. Matrice de paires de types de marchandises incompatibles (ex. plan :
 *     miroirs vs graviers/bois), par typeCatalogue (reference au catalogue
 *     d'emballages EF-MKT-03/04 - la classe de marchandise la plus proche
 *     disponible dans le contrat DemandePublieeLots). Chaine libre, pas
 *     d'enum : un nouveau type ne doit jamais exiger de redeploiement.
 *     Pilotee par la propriete :
 *     fretcorridor.opt.compatibilite-marchandises.paires-incompatibles
 *     = liste de paires ["typeA,typeB"], casse-insensible, chaque paire
 *     symetrique (si A incompatible avec B, B l'est avec A).
 *
 * Degradation permissive par defaut : si aucune configuration n'est fournie
 * (ou la matrice est vide), AUCUNE incompatibilite n'est appliquee -
 * comportement historique preserve, coherent avec le principe deja applique
 * sur rayonAppariementKm/detourMaxDistanceKm (mode permissif par defaut).
 */
@Service
public class CompatibiliteMarchandisesService {

    private static final Logger log = LoggerFactory.getLogger(CompatibiliteMarchandisesService.class);

    private final AffectationRepository affectationRepository;
    private final LotDemandeRepository lotDemandeRepository;
    private final boolean antiGroupageMatieresDangereuses;
    private final List<String[]> pairesIncompatibles;

    public CompatibiliteMarchandisesService(
            AffectationRepository affectationRepository,
            LotDemandeRepository lotDemandeRepository,
            @Value("${fretcorridor.opt.compatibilite-marchandises.anti-groupage-matieres-dangereuses:true}")
            boolean antiGroupageMatieresDangereuses,
            @Value("${fretcorridor.opt.compatibilite-marchandises.paires-incompatibles:}")
            List<String> pairesIncompatiblesConfig) {
        this.affectationRepository = affectationRepository;
        this.lotDemandeRepository = lotDemandeRepository;
        this.antiGroupageMatieresDangereuses = antiGroupageMatieresDangereuses;
        this.pairesIncompatibles = normaliserPaires(pairesIncompatiblesConfig);
        if (!this.pairesIncompatibles.isEmpty()) {
            log.info("Matrice d'incompatibilite marchandises : {} paire(s) configuree(s), "
                            + "anti-groupage matieres dangereuses={}",
                    this.pairesIncompatibles.size(), antiGroupageMatieresDangereuses);
        }
    }

    /**
     * Filtre dure PRE-L1 : la demande candidate est-elle compatible avec les
     * demandes deja confirmees sur la capacite donnee ?
     *
     * @return false si la demande candidate porte une marchandise
     *         incompatible avec AU MOINS UNE demande deja sur la capacite -
     *         le candidat doit etre exclu avant le calcul de cout MAT.
     */
    public boolean compatibleAvecDemandesDeLaCapacite(UUID demandeId, UUID capaciteId) {
        List<Affectation> dejaSurCapacite = demandesDejaConfirmeesSurCapacite(capaciteId);
        if (dejaSurCapacite.isEmpty()) {
            return true; // rien en cours sur la capacite - aucune incompatibilite possible
        }
        return compatible(demandeId, dejaSurCapacite.stream().map(Affectation::getDemandeId).toList());
    }

    /**
     * Compatibilite entre une demande candidate et une liste de demandes deja
     * presentes (confirmees sur la meme capacite, ou toute autre collection).
     * Symetrique et associative : verifie la demande candidate contre chacune
     * des demandes deja presentes.
     */
    public boolean compatible(UUID demandeCandidate, List<UUID> demandesDejaPresentes) {
        if (demandesDejaPresentes == null || demandesDejaPresentes.isEmpty()) {
            return true;
        }
        List<LotDemande> lotsCandidats = lotDemandeRepository.findByDemandeId(demandeCandidate);
        if (lotsCandidats.isEmpty()) {
            // Pas de detail lot pour la demande candidate : rien a verifier -
            // mode permissif (donnees marchandise incompletes ne bloquent pas,
            // meme principe que l'oracle de chargement).
            return true;
        }
        for (UUID demandeExistante : demandesDejaPresentes) {
            if (demandeExistante.equals(demandeCandidate)) {
                continue; // ne jamais comparer une demande a elle-meme
            }
            List<LotDemande> lotsExistants = lotDemandeRepository.findByDemandeId(demandeExistante);
            if (lotsExistants.isEmpty()) {
                continue; // pas de detail lot chez l'existant - permissif aussi
            }
            if (!compatibles(lotsCandidats, lotsExistants)) {
                return false;
            }
        }
        return true;
    }

    // --- Grace ---

    private List<Affectation> demandesDejaConfirmeesSurCapacite(UUID capaciteId) {
        // Capable d'acceder a StatutAffectation (package-prive) car ce service
        // vit dans le meme package com.fretcorridor.opt.domain.
        return affectationRepository.findByCapaciteIdAndStatut(capaciteId, StatutAffectation.CONFIRMEE);
    }

    private boolean compatibles(List<LotDemande> lotsA, List<LotDemande> lotsB) {
        if (antiGroupageMatieresDangereuses && porteMatiereDangereuse(lotsA) && porteMatiereDangereuse(lotsB)) {
            log.info("Incompatibilite EF-MKT-10 (anti-groupage matieres dangereuses) - "
                    + "{} lot(s) dangereux(s) des deux cotes.",
                    lotsA.size() + lotsB.size());
            return false;
        }
        Set<String> typesA = typesDe(lotsA);
        Set<String> typesB = typesDe(lotsB);
        for (String typeA : typesA) {
            for (String typeB : typesB) {
                if (paireIncompatible(typeA, typeB)) {
                    log.info("Incompatibilite marchandises (matrice) - '{}' vs '{}'",
                            typeA, typeB);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean porteMatiereDangereuse(List<LotDemande> lots) {
        return lots.stream().anyMatch(lot -> lot.getClasseDanger() != null && !lot.getClasseDanger().isBlank());
    }

    private Set<String> typesDe(List<LotDemande> lots) {
        Set<String> types = new HashSet<>();
        for (LotDemande lot : lots) {
            if (lot.getTypeCatalogue() != null && !lot.getTypeCatalogue().isBlank()) {
                types.add(lot.getTypeCatalogue().trim().toLowerCase(Locale.ROOT));
            }
        }
        return types;
    }

    private boolean paireIncompatible(String typeA, String typeB) {
        if (typeA == null || typeB == null) {
            return false;
        }
        for (String[] paire : pairesIncompatibles) {
            boolean premier = (paire[0].equals(typeA) && paire[1].equals(typeB))
                    || (paire[0].equals(typeB) && paire[1].equals(typeA));
            if (premier) {
                return true;
            }
        }
        return false;
    }

    private List<String[]> normaliserPaires(List<String> config) {
        List<String[]> paires = new ArrayList<>();
        if (config == null) {
            return paires;
        }
        for (String entree : config) {
            if (entree == null || entree.isBlank()) {
                continue;
            }
            String[] secondes = entree.split(",", -1);
            if (secondes.length != 2 || secondes[0].isBlank() || secondes[1].isBlank()) {
                log.warn("Paire d'incompatibilite marchandises ignoree (format attendu 'typeA,typeB') : '{}'",
                        entree);
                continue;
            }
            paires.add(new String[]{
                    secondes[0].trim().toLowerCase(Locale.ROOT),
                    secondes[1].trim().toLowerCase(Locale.ROOT)
            });
        }
        return paires;
    }
}