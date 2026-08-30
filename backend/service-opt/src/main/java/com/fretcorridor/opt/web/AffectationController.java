package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.StatutAffectation;
import com.fretcorridor.opt.web.dto.AffectationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
 *
 * BUG CORRIGE (audit de suivi, 23 aout) : ce endpoint restait permitAll()
 * SANS aucune verification de cle interne (contrairement a tous les autres
 * endpoints internes du systeme, ex. CoutController/calculer-lot cote
 * service-mat) - protege jusqu'ici par la seule imprevisibilite de l'UUID.
 * Meme pattern X-Internal-Service-Key que CoutController desormais applique
 * ici ; ServiceOptClient (service-trk) transporte la cle.
 */
@RestController
@RequestMapping("/api/opt/affectations")
public class AffectationController {

    private final AffectationRepository affectationRepository;
    private final String cleInterneAttendue;

    public AffectationController(AffectationRepository affectationRepository,
                                  @Value("${fretcorridor.internal.service-key}") String cleInterneAttendue) {
        this.affectationRepository = affectationRepository;
        this.cleInterneAttendue = cleInterneAttendue;
    }

    @GetMapping("/{missionId}")
    public AffectationResponse consulter(@PathVariable UUID missionId,
                                          @RequestHeader(value = "X-Internal-Service-Key", required = false)
                                          String cleInterne) {
        if (!cleInterneAttendue.equals(cleInterne)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cle interne invalide ou absente");
        }
        Affectation affectation = affectationRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Affectation introuvable : " + missionId));
        return AffectationResponse.from(affectation);
    }

    /**
     * Diffusion-course (trouvaille Mobile) : "mes propositions en attente"
     * d'un transporteur. Consomme par service-cap (backend Mobile, en
     * back-to-back avec la cle interne, meme patron que TRK ci-dessus) pour
     * alimenter l'ecran propositions du chauffeur, et les expirees sans
     * re-diffuser.
     *
     * Retourne uniquement les Affectation a l'etat PROPOSEE du transporteur :
     * une mission CONFIRMEE (en cours) ou EXPIREE n'est plus "a traiter".
     * Le filtre statistiquement le plus selectif (transporteur + statut) est
     * couvert par l'index idx_affectation_transporteur_statut (V27).
     */
    @GetMapping("/proposees")
    public List<AffectationResponse> listerProposees(
            @RequestParam UUID transporteurId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String cleInterne) {
        if (!cleInterneAttendue.equals(cleInterne)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cle interne invalide ou absente");
        }
        return affectationRepository
                .findByTransporteurIdAndStatut(transporteurId, StatutAffectation.PROPOSEE)
                .stream()
                .map(AffectationResponse::from)
                .toList();
    }
}
