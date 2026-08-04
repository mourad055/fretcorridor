package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.AffectationL1Service;
import com.fretcorridor.opt.domain.AffectationLotResultat;
import com.fretcorridor.opt.domain.DemandeAvecCandidats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint de verification manuelle du L1 (affectation Kuhn-Munkres, Sprint 5).
 *
 * Comme FiltrageL0Controller pour le L0 : declenchement HTTP direct conserve
 * uniquement pour les tests de la Phase 1 / Sprint 5, disparaitra au profit
 * d'un declenchement evenementiel une fois le module valide de bout en bout.
 */
@RestController
@RequestMapping("/api/opt/affectation-l1")
public class AffectationL1Controller {

    private final AffectationL1Service affectationL1Service;

    public AffectationL1Controller(AffectationL1Service affectationL1Service) {
        this.affectationL1Service = affectationL1Service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AffectationLotResultat calculerAffectation(@Valid @RequestBody RequeteAffectationL1 requete) {
        return affectationL1Service.calculerAffectationOptimale(requete.demandes());
    }

    public record RequeteAffectationL1(
            @NotNull
            @NotEmpty
            @Size(max = 50, message = "50 demandes maximum par lot L1")
            List<DemandeAvecCandidats> demandes
    ) {
    }
}
