package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.web.dto.AffectationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Point d'entree synchrone interne pour TRK (Sprint 6) - comble le trou
 * d'architecture identifie : TRK n'a aucun autre moyen de connaitre
 * origine/destination d'une mission avant de calculer son ETA.
 *
 * Appel synchrone justifie par la regle de communication du perimetre Moteur
 * (Plan d'execution S4.2) : MAT<->OPT<->GEO<->TRK = meme porteur, meme
 * contexte de deploiement. A ne jamais confondre avec l'evenement Kafka
 * AffectationConfirmee (OPT -> Mobile/service-exe, cross-porteur, strictement
 * asynchrone) - deux mecanismes distincts pour deux consommateurs distincts,
 * meme si tous deux portent le meme missionId.
 */
@RestController
@RequestMapping("/api/opt/affectations")
public class AffectationController {

    private final AffectationRepository affectationRepository;

    public AffectationController(AffectationRepository affectationRepository) {
        this.affectationRepository = affectationRepository;
    }

    @GetMapping("/{missionId}")
    public AffectationResponse consulter(@PathVariable UUID missionId) {
        Affectation affectation = affectationRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Affectation introuvable : " + missionId));
        return AffectationResponse.from(affectation);
    }
}
