package com.fretcorridor.mat.web;

import com.fretcorridor.mat.domain.CandidatCout;
import com.fretcorridor.mat.domain.CoutCompositeService;
import com.fretcorridor.mat.web.dto.CoutLotRequest;
import com.fretcorridor.mat.web.dto.CoutLotResponse;
import com.fretcorridor.mat.web.dto.PaireCandidatRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * API interne de calcul de cout composite (EF-MAT-04), consommee en synchrone
 * REST par service-opt (meme porteur, budget L0 ~50ms) pour construire la
 * matrice de couts avant l'affectation Kuhn-Munkres (L1).
 */
@RestController
@RequestMapping("/api/mat/couts")
public class CoutController {

    private final CoutCompositeService coutCompositeService;

    public CoutController(CoutCompositeService coutCompositeService) {
        this.coutCompositeService = coutCompositeService;
    }

    @PostMapping("/calculer-lot")
    @ResponseStatus(HttpStatus.OK)
    public CoutLotResponse calculerLot(@Valid @RequestBody CoutLotRequest request) {
        validerValeursCriteres(request);

        List<CandidatCout> candidats = request.candidats().stream()
                .map(paire -> new CandidatCout(paire.capaciteId(), paire.valeursCriteres()))
                .toList();

        return coutCompositeService.calculerCoutsLot(request.demandeId(), candidats);
    }

    /**
     * Controle manuel au-dela de ce que Bean Validation exprime facilement sur
     * les valeurs d'une Map (pas de validateur standard pour "chaque valeur
     * d'une Map<String,Double> doit etre finie et dans [0,1]") : rejet explicite
     * en 400 plutot que de laisser une valeur aberrante (NaN, Infinity, hors
     * echelle) fausser silencieusement un cout ensuite utilise pour Kuhn-Munkres.
     */
    private void validerValeursCriteres(CoutLotRequest request) {
        for (PaireCandidatRequest candidat : request.candidats()) {
            for (Map.Entry<String, Double> entree : candidat.valeursCriteres().entrySet()) {

                if (!entree.getKey().matches("^[A-Z_]{2,50}$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Code critere invalide : " + entree.getKey()
                                    + " (attendu : majuscules et underscores uniquement)");
                }

                Double valeur = entree.getValue();
                if (valeur == null || !Double.isFinite(valeur) || valeur < 0.0 || valeur > 1.0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Valeur de critere hors limites pour " + entree.getKey()
                                    + " (attendu : nombre fini entre 0 et 1, recu " + valeur + ")");
                }
            }
        }
    }
}
