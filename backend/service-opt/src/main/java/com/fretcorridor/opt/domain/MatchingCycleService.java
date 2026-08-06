package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.AxeActifDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .map(c -> new CandidatCoutDto(c.getCapaciteId(), c.getValeursCriteres(),
                        c.getPosition(), c.getProfilCamion(), c.getTypeVehicule()))
                .toList();

        List<DemandeAvecCandidats> lot = demandes.stream()
                .map(d -> new DemandeAvecCandidats(d.getDemandeId(), d.getOrigine(), d.getDestination(),
                        d.getAxeId(), d.getPoidsTaxableKg(), candidatsCommuns))
                .toList();

        log.info("Cycle de matching declenche - axe={}, {} demande(s), {} capacite(s) en attente",
                axe.nom(), demandes.size(), capacites.size());

        AffectationLotResultat resultat = affectationL1Service.calculerAffectationOptimale(lot);

        if (resultat.modeDegrade()) {
            log.warn("Cycle en mode degrade sur l'axe {} - service-mat injoignable, "
                    + "capacites/demandes laissees en attente pour le prochain tour.", axe.nom());
            return; // ne marque rien comme traite : on retentera au cycle suivant (ENF-DIS-04)
        }

        demandes.forEach(DemandeEnAttente::marquerTraitee);
        demandeEnAttenteRepository.saveAll(demandes);

        capacites.forEach(CapaciteEnAttente::marquerTraitee);
        capaciteEnAttenteRepository.saveAll(capacites);
    }
}
