package com.fretcorridor.gateway.infrastructure.rest.trk;

import com.fretcorridor.gateway.domain.trk.TrkPort;
import com.fretcorridor.gateway.infrastructure.rest.trk.dto.PositionResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * FE-TRK-04 / RG-043 (Sprint 6) : un Bureau voit les positions de son propre
 * tenant, toujours accompagnées de leur âge (ENF-MUL-01, ENF-PRF-02).
 */
@RestController
public class PositionController {

    private final TrkPort trkPort;

    public PositionController(TrkPort trkPort) {
        this.trkPort = trkPort;
    }

    @GetMapping("/api/v1/bureau/positions")
    public Flux<PositionResponse> positions(@AuthenticationPrincipal AuthenticatedActor actor) {
        return trkPort.listerPositionsParTenant(actor.tenantId()).map(PositionResponse::from);
    }
}
