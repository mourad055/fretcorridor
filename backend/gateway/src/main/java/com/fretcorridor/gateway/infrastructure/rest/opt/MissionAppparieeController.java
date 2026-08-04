package com.fretcorridor.gateway.infrastructure.rest.opt;

import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.MissionAppparieeResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * FE-BUR-01 (Sprint 5, missions appariées) : un Bureau ne voit que les missions
 * de son propre tenant — le tenantId vient exclusivement du JWT (ENF-MUL-01).
 */
@RestController
public class MissionAppparieeController {

    private final OptPort optPort;

    public MissionAppparieeController(OptPort optPort) {
        this.optPort = optPort;
    }

    @GetMapping("/api/v1/bureau/missions-appariees")
    public Flux<MissionAppparieeResponse> missionsAppariees(@AuthenticationPrincipal AuthenticatedActor actor) {
        return optPort.listerMissionsParTenant(actor.tenantId()).map(MissionAppparieeResponse::from);
    }
}
