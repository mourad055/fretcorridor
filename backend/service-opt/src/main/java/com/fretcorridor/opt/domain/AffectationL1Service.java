package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.algorithm.KuhnMunkresSolver;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotRequestDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ServiceMatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestration du L1 (Sprint 5, EF-MAT-01/02/03) : appariement optimal par
 * lots via Kuhn-Munkres, JAMAIS glouton (anti-patron explicite du CDC).
 *
 * Construit la matrice de cout demande x capacite en appelant service-mat une
 * fois par demande (coherent avec le contrat CoutLotRequest cote MAT : cout
 * d'un lot de candidats face a UNE demande), puis resout l'affectation
 * optimale sur l'ensemble du lot d'un coup.
 *
 * Toutes les demandes du lot DOIVENT partager exactement le meme ensemble de
 * candidats - coherent avec le fait qu'elles proviennent du meme filtrage L0
 * (meme zone H3) : sinon une colonne "capacite" ne designerait pas la meme
 * capacite sur toutes les lignes et la matrice n'aurait pas de sens.
 */
@Service
public class AffectationL1Service {

    private static final Logger log = LoggerFactory.getLogger(AffectationL1Service.class);

    private final ServiceMatClient serviceMatClient;

    public AffectationL1Service(ServiceMatClient serviceMatClient) {
        this.serviceMatClient = serviceMatClient;
    }

    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes) {
        if (demandes == null || demandes.isEmpty()) {
            return new AffectationLotResultat(false, List.of());
        }

        List<UUID> capacitesReference = demandes.get(0).candidats().stream()
                .map(CandidatCoutDto::capaciteId)
                .toList();
        Set<UUID> capacitesReferenceSet = new LinkedHashSet<>(capacitesReference);

        for (DemandeAvecCandidats demande : demandes) {
            Set<UUID> capacitesDemande = new LinkedHashSet<>();
            demande.candidats().forEach(c -> capacitesDemande.add(c.capaciteId()));
            if (!capacitesDemande.equals(capacitesReferenceSet)) {
                throw new IllegalArgumentException(
                        "Toutes les demandes du lot L1 doivent partager exactement le meme "
                                + "ensemble de capacites candidates (issu du meme filtrage L0). "
                                + "Ecart detecte sur la demande " + demande.demandeId());
            }
        }

        int nbDemandes = demandes.size();
        int nbCapacites = capacitesReference.size();
        double[][] matriceCouts = new double[nbDemandes][nbCapacites];
        UUID[][] cycleMatchingIds = new UUID[nbDemandes][nbCapacites];

        for (int i = 0; i < nbDemandes; i++) {
            DemandeAvecCandidats demande = demandes.get(i);

            CoutLotResponseDto reponse = serviceMatClient.calculerCoutsLot(
                    new CoutLotRequestDto(demande.demandeId(), demande.candidats()));

            if (reponse == null) {
                log.warn("service-mat injoignable pour la demande {} - lot L1 en mode degrade, "
                        + "aucune affectation produite.", demande.demandeId());
                return new AffectationLotResultat(true, List.of());
            }

            List<CoutResponseDto> resultats = reponse.resultats();
            for (int j = 0; j < nbCapacites; j++) {
                matriceCouts[i][j] = resultats.get(j).coutTotal().doubleValue();
                cycleMatchingIds[i][j] = resultats.get(j).cycleMatchingId();
            }
        }

        int[] affectationParDemande = KuhnMunkresSolver.resoudre(matriceCouts);

        List<AffectationResultat> resultatsFinaux = new ArrayList<>(nbDemandes);
        for (int i = 0; i < nbDemandes; i++) {
            int indiceCapacite = affectationParDemande[i];
            UUID demandeId = demandes.get(i).demandeId();

            if (indiceCapacite == -1) {
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null));
            } else {
                UUID capaciteId = capacitesReference.get(indiceCapacite);
                resultatsFinaux.add(new AffectationResultat(
                        demandeId,
                        capaciteId,
                        BigDecimal.valueOf(matriceCouts[i][indiceCapacite]),
                        cycleMatchingIds[i][indiceCapacite]));
            }
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }
}
