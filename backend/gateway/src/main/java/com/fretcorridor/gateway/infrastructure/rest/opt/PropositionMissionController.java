package com.fretcorridor.gateway.infrastructure.rest.opt;

import com.fretcorridor.gateway.domain.opt.PropositionMissionPort;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.PropositionMissionResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * UC-MAT-02 du CDC (page 43) : "Mes propositions" côté app Chauffeur/Transporteur.
 * Voir RealPropositionMissionAdapter pour la mise en garde architecturale
 * (entorse temporaire, appel direct vers service-opt).
 */
@RestController
@RequestMapping("/api/v1/propositions-mission")
public class PropositionMissionController {

    private final PropositionMissionPort propositionMissionPort;

    public PropositionMissionController(PropositionMissionPort propositionMissionPort) {
        this.propositionMissionPort = propositionMissionPort;
    }

    @GetMapping("/mes")
    public Flux<PropositionMissionResponse> mesPropositions(@AuthenticationPrincipal AuthenticatedActor actor) {
        return propositionMissionPort.mesPropositions(UUID.fromString(actor.actorId()))
                .map(PropositionMissionResponse::from);
    }

    @PostMapping("/{id}/accepter")
    public Mono<PropositionMissionResponse> accepter(@PathVariable UUID id,
                                                        @AuthenticationPrincipal AuthenticatedActor actor) {
        return propositionMissionPort.accepter(id, UUID.fromString(actor.actorId()))
                .map(PropositionMissionResponse::from);
    }

    @PostMapping("/{id}/refuser")
    public Mono<PropositionMissionResponse> refuser(@PathVariable UUID id,
                                                       @RequestBody(required = false) RefuserRequest requete,
                                                       @AuthenticationPrincipal AuthenticatedActor actor) {
        String motif = requete != null ? requete.motif() : null;
        return propositionMissionPort.refuser(id, UUID.fromString(actor.actorId()), motif)
                .map(PropositionMissionResponse::from);
    }

    public record RefuserRequest(String motif) {
    }
}
