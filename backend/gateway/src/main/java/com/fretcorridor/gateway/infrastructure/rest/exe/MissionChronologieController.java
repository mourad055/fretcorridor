package com.fretcorridor.gateway.infrastructure.rest.exe;

import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chronologie de mission (Sprint 7). Un Bureau supervise son territoire
 * (ENF-MUL-01) ; un Transporteur ne voit que ses propres missions (PRD §5.3).
 */
@RestController
public class MissionChronologieController {

    private final ExePort exePort;

    public MissionChronologieController(ExePort exePort) {
        this.exePort = exePort;
    }

    @GetMapping("/api/v1/bureau/missions-chronologie")
    public Flux<MissionResponse> chronologieBureau(@AuthenticationPrincipal AuthenticatedActor actor) {
        return exePort.listerMissionsParTenant(actor.tenantId(), actor.delegationToken()).map(MissionResponse::from);
    }

    @GetMapping("/api/v1/transporteur/missions")
    public Flux<MissionResponse> missionsTransporteur(@AuthenticationPrincipal AuthenticatedActor actor) {
        return exePort.listerMissionsParTransporteur(actor.tenantId(), actor.actorId(), actor.delegationToken())
                .map(MissionResponse::from);
    }
}
