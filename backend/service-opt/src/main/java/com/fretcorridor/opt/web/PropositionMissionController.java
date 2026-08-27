package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.PropositionMission;
import com.fretcorridor.opt.domain.PropositionMissionService;
import com.fretcorridor.opt.web.dto.PropositionMissionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * UC-MAT-02 du CDC. Meme garde X-Internal-Service-Key que AffectationController
 * -- la resolution de "qui est le transporteur qui appelle" reste la
 * responsabilite de la gateway (JWT), jamais de service-opt lui-meme (aucun
 * autre endpoint de ce service ne parse de JWT).
 */
@RestController
@RequestMapping("/api/opt/propositions-mission")
public class PropositionMissionController {

    private final PropositionMissionService propositionMissionService;
    private final String cleInterneAttendue;

    public PropositionMissionController(PropositionMissionService propositionMissionService,
                                         @Value("${fretcorridor.internal.service-key}") String cleInterneAttendue) {
        this.propositionMissionService = propositionMissionService;
        this.cleInterneAttendue = cleInterneAttendue;
    }

    @GetMapping("/mes")
    public List<PropositionMissionResponse> mesPropositions(
            @RequestParam UUID transporteurId,
            @RequestHeader("X-Internal-Service-Key") String cleInterne) {
        verifierCle(cleInterne);
        return propositionMissionService.mesPropositions(transporteurId).stream()
                .map(PropositionMissionResponse::from).toList();
    }

    @PostMapping("/{id}/accepter")
    public PropositionMissionResponse accepter(
            @PathVariable UUID id,
            @RequestParam UUID transporteurId,
            @RequestHeader("X-Internal-Service-Key") String cleInterne) {
        verifierCle(cleInterne);
        return PropositionMissionResponse.from(propositionMissionService.accepter(id, transporteurId));
    }

    @PostMapping("/{id}/refuser")
    public PropositionMissionResponse refuser(
            @PathVariable UUID id,
            @RequestParam UUID transporteurId,
            @RequestBody(required = false) RefuserRequest requete,
            @RequestHeader("X-Internal-Service-Key") String cleInterne) {
        verifierCle(cleInterne);
        String motif = requete != null ? requete.motif() : null;
        return PropositionMissionResponse.from(propositionMissionService.refuser(id, transporteurId, motif));
    }

    private void verifierCle(String cleInterne) {
        if (!cleInterneAttendue.equals(cleInterne)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cle interne invalide ou absente");
        }
    }

    public record RefuserRequest(String motif) {
    }
}
