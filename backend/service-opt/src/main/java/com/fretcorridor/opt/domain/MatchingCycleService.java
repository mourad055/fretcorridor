package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.AxeActifDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Declencheur du cycle de matching "par fenetre", par axe actif (EF-MAT-01 -
 * jamais un matching immediat evenement par evenement). Tourne
 * periodiquement (spring.matching.cycle-interval-ms), regroupe tout ce qui
 * est en attente sur chaque axe ou matchingActif=true (EF-GEO-03), et lance
 * un seul L1 (Kuhn-Munkres) par axe sur le lot entier.
 *
 * Un axe sans demande ET sans capacite en attente est simplement ignore ce
 * tour - pas d'appel MAT/Kuhn-Munkres inutile (cf ServiceMatClient, evite de
 * gaspiller un cycle sur un lot vide).
 *
 * Un axe avec des demandes mais aucune capacite (ou l'inverse) reste lui
 * aussi en attente : AffectationL1Service exige que candidats soit non vide,
 * donc rien n'est tente tant que les deux cotes n'ont pas au moins une
 * entree - comportement voulu, pas une limitation a corriger.
 */
@Service
public class MatchingCycleService {

    private static final Logger log = LoggerFactory.getLogger(MatchingCycleService.class);

    private final ServiceGeoClient serviceGeoClient;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;
    private final AffectationL1Service affectationL1Service;

    public MatchingCycleService(ServiceGeoClient serviceGeoClient,
                                 CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                 DemandeEnAttenteRepository demandeEnAttenteRepository,
                                 AffectationL1Service affectationL1Service) {
        this.serviceGeoClient = serviceGeoClient;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
        this.affectationL1Service = affectationL1Service;
    }

    @Scheduled(fixedDelayString = "${spring.matching.cycle-interval-ms:15000}")
    public void executerCycle() {
        List<AxeActifDto> axesActifs = serviceGeoClient.axesActifsMatching();

        if (axesActifs.isEmpty()) {
            log.debug("Aucun axe actif pour le matching ce tour (ou service-geo injoignable).");
            return;
        }

        for (AxeActifDto axe : axesActifs) {
            traiterAxe(axe);
        }
    }

    private void traiterAxe(AxeActifDto axe) {
        List<CapaciteEnAttente> capacites = capaciteEnAttenteRepository.findByAxeIdAndTraiteeFalse(axe.id());
        List<DemandeEnAttente> demandes = demandeEnAttenteRepository.findByAxeIdAndTraiteeFalse(axe.id());

        if (capacites.isEmpty() || demandes.isEmpty()) {
            // Rien a apparier ce tour sur cet axe - reste en attente pour le prochain.
            return;
        }

        List<CandidatCoutDto> candidatsCommuns = capacites.stream()
                .map(c -> new CandidatCoutDto(c.getCapaciteId(), c.getTransporteurId(),
                        c.getVehiculeId(), c.getValeursCriteres(),
                        c.getPosition(), c.getProfilCamion(), c.getTypeVehicule()))
                .toList();

        // EF-MAT-01/02/03 (Phase 1 MVP, priorite M) - RG-046 : rayonMatchingKm
        // lu depuis Axe.parametres (EF-GEO-02), jamais code en dur. Absence
        // de cle = pas de borne appliquee ce cycle (limitation documentee
        // dans AxeActifDto, pas une valeur par defaut inventee ici).
        //
        // CHOIX D'EQUIPE (le CDC ne precise pas explicitement "distance entre
        // quoi et quoi") : rayon autour de l'ORIGINE de la demande - RG-046
        // parle de "distance d'approche a vide", c-a-d la distance camion ->
        // point de collecte, pas la destination ni le trajet complet.
        // Coherent avec ZonageH3Service/hubs-proches (GEO), qui filtre deja
        // par rayon autour d'un point de collecte unique.
        Double rayonMatchingKm = extraireRayonMatchingKm(axe);

        List<DemandeAvecCandidats> lot = demandes.stream()
                .map(d -> new DemandeAvecCandidats(d.getDemandeId(), d.getOrigine(), d.getDestination(),
                        d.getAxeId(), d.getPoidsTaxableKg(), filtrerCandidatsParRayon(d, candidatsCommuns, rayonMatchingKm)))
                .toList();

        // Une demande dont TOUS les candidats ont ete elimines par le rayon
        // n'a plus de sens a envoyer a Kuhn-Munkres (AffectationL1Service
        // exige des candidats non vides par demande) - filtree du lot plutot
        // que de faire planter le L1 sur une liste vide.
        List<DemandeAvecCandidats> lotNonVide = lot.stream()
                .filter(d -> !d.candidats().isEmpty())
                .toList();

        if (lotNonVide.isEmpty()) {
            log.debug("Aucun candidat dans le rayon d'appariement sur l'axe {} ce tour - "
                    + "demandes/capacites laissees en attente pour le prochain cycle.", axe.nom());
            return;
        }

        log.info("Cycle de matching declenche - axe={}, {} demande(s), {} capacite(s) en attente "
                        + "(rayon={} km)",
                axe.nom(), demandes.size(), capacites.size(), rayonMatchingKm);

        AffectationLotResultat resultat = affectationL1Service.calculerAffectationOptimale(lotNonVide);

        if (resultat.modeDegrade()) {
            log.warn("Cycle en mode degrade sur l'axe {} - service-mat injoignable, "
                    + "capacites/demandes laissees en attente pour le prochain tour.", axe.nom());
            return; // ne marque rien comme traite : on retentera au cycle suivant (ENF-DIS-04)
        }

        // BUG CORRIGE (2026-08-18) : le code precedent marquait TOUT le lot
        // (demandes ET capacites) comme traite des que le resultat n'etait
        // pas en mode degrade, sans verifier lesquelles avaient reellement
        // ete affectees. Kuhn-Munkres etant un appariement optimal, un lot
        // avec plus de demandes que de capacites (cas courant) laisse
        // certaines demandes avec capaciteId()==null (cf AffectationResultat
        // javadoc, "plus de demandes que de capacites disponibles dans le
        // lot") - ces demandes etaient neanmoins marquees traitee=true et
        // disparaissaient silencieusement du systeme, en violation de
        // EF-MAT-01 (traitement par cycles a fenetre, jamais de perte) et
        // EF-MAT-11/12 (traçabilite/reconstitution de chaque decision).
        Set<java.util.UUID> demandesAffectees = resultat.affectations().stream()
                .filter(a -> a.capaciteId() != null)
                .map(AffectationResultat::demandeId)
                .collect(Collectors.toSet());
        Set<java.util.UUID> capacitesAffectees = resultat.affectations().stream()
                .filter(a -> a.capaciteId() != null)
                .map(AffectationResultat::capaciteId)
                .collect(Collectors.toSet());

        List<DemandeEnAttente> demandesTraitees = demandes.stream()
                .filter(d -> demandesAffectees.contains(d.getDemandeId()))
                .toList();
        demandesTraitees.forEach(DemandeEnAttente::marquerTraitee);
        demandeEnAttenteRepository.saveAll(demandesTraitees);

        List<CapaciteEnAttente> capacitesTraitees = capacites.stream()
                .filter(c -> capacitesAffectees.contains(c.getCapaciteId()))
                .toList();
        capacitesTraitees.forEach(CapaciteEnAttente::marquerTraitee);
        capaciteEnAttenteRepository.saveAll(capacitesTraitees);

        int demandesNonAffecteesCeCycle = demandes.size() - demandesTraitees.size();
        if (demandesNonAffecteesCeCycle > 0) {
            log.info("Cycle sur l'axe {} : {} demande(s) non affectee(s) ce tour (plus de demandes "
                            + "que de capacites disponibles), laissee(s) en attente pour le prochain cycle.",
                    axe.nom(), demandesNonAffecteesCeCycle);
        }
    }

    /**
     * Lit "rayonAppariementKm" dans Axe.parametres (EF-GEO-02, JSONB jamais code
     * en dur). Retourne null si la cle est absente ou de type inattendu -
     * dans ce cas aucun filtre n'est applique, comportement volontairement
     * permissif plutot qu'un defaut invente.
     */
    private Double extraireRayonMatchingKm(AxeActifDto axe) {
        Map<String, Object> parametres = axe.parametres();
        if (parametres == null) {
            return null;
        }
        Object valeur = parametres.get("rayonAppariementKm");
        if (valeur instanceof Number nombre) {
            return nombre.doubleValue();
        }
        if (valeur != null) {
            log.warn("rayonAppariementKm present sur l'axe {} mais de type inattendu ({}) - "
                    + "filtre ignore ce tour.", axe.nom(), valeur.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Filtre les candidats dont la position est a plus de rayonKm de
     * l'origine de la demande (Haversine, RG-046 : "distance d'approche a
     * vide"). Aucun filtre applique si rayonKm est null (cle absente) ou si
     * l'origine de la demande / la position d'un candidat est inconnue -
     * degrade vers "pas de borne" plutot que d'exclure silencieusement un
     * candidat sur une donnee manquante (ENF-DIS-04).
     */
    private List<CandidatCoutDto> filtrerCandidatsParRayon(DemandeEnAttente demande,
                                                            List<CandidatCoutDto> candidats,
                                                            Double rayonKm) {
        PointGeoDto origine = demande.getOrigine();
        if (rayonKm == null || origine == null) {
            return candidats;
        }

        return candidats.stream()
                .filter(c -> {
                    PointGeoDto position = c.positionCapacite();
                    if (position == null) {
                        return true; // position inconnue : ne pas exclure silencieusement
                    }
                    return HaversineUtils.distance(origine, position) <= rayonKm;
                })
                .toList();
    }
}
