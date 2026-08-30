package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.PropositionCapPort;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.PropositionCapResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

/**
 * UC-MAT-02/diffusion-course (30/08) : "mes propositions" côté app
 * Chauffeur/Transporteur, relayé vers service-cap (voir
 * RealPropositionCapAdapter, entorse synchrone deliberee et bilaterale avec
 * le Moteur, documentee cote service-cap).
 */
@RestController
public class PropositionCapController {

    private final PropositionCapPort propositionCapPort;

    public PropositionCapController(PropositionCapPort propositionCapPort) {
        this.propositionCapPort = propositionCapPort;
    }

    @GetMapping("/api/v1/transporteur/propositions")
    public Flux<PropositionCapResponse> mesPropositions(@AuthenticationPrincipal AuthenticatedActor actor) {
        return propositionCapPort.mesPropositions(actor.delegationToken()).map(PropositionCapResponse::from);
    }

    @PostMapping("/api/v1/transporteur/propositions/{affectationId}/accepter")
    public Mono<ResponseEntity<Void>> accepter(@PathVariable UUID affectationId,
                                                @RequestBody RepondreRequest requete,
                                                @AuthenticationPrincipal AuthenticatedActor actor) {
        return propositionCapPort.accepter(affectationId, requete.demandeId(), requete.capaciteId(), actor.delegationToken())
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/api/v1/transporteur/propositions/{affectationId}/refuser")
    public Mono<ResponseEntity<Void>> refuser(@PathVariable UUID affectationId,
                                               @RequestBody RepondreRequest requete,
                                               @AuthenticationPrincipal AuthenticatedActor actor) {
        return propositionCapPort.refuser(affectationId, requete.demandeId(), requete.capaciteId(), actor.delegationToken())
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    public record RepondreRequest(UUID demandeId, UUID capaciteId) {
    }
}
