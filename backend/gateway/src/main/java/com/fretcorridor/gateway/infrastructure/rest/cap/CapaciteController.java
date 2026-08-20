package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.CapacitePort;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.CapaciteResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * FE-TRP-01 (Sprint 4, lecture seule) : un Transporteur ne voit que ses propres
 * capacités déclarées — le transporteurId vient exclusivement du JWT, jamais
 * d'un paramètre de requête (PRD §5.3, périmètre strict par acteur).
 */
@RestController
public class CapaciteController {

    private final CapacitePort capacitePort;

    public CapaciteController(CapacitePort capacitePort) {
        this.capacitePort = capacitePort;
    }

    @GetMapping("/api/v1/transporteur/capacites")
    public Flux<CapaciteResponse> capacites(@AuthenticationPrincipal AuthenticatedActor actor) {
        return capacitePort.listerParTransporteur(actor.actorId()).map(CapaciteResponse::from);
    }
}
