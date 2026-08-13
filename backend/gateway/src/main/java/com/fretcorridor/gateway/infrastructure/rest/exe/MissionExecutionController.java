package com.fretcorridor.gateway.infrastructure.rest.exe;

import com.fretcorridor.gateway.domain.exe.MissionExecutionPort;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.AjouterEtapeRequest;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionExecutionDetailResponse;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionExecutionResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * S7 (EF-EXE-02/04) : exécution de mission côté chauffeur/transporteur.
 * Reste vide en pratique tant que transporteurId n'est pas peuplé en amont
 * (écart documenté, cf. MissionExecutionPort).
 */
@RestController
@RequestMapping("/api/v1/missions")
public class MissionExecutionController {

    private final MissionExecutionPort missionExecutionPort;

    public MissionExecutionController(MissionExecutionPort missionExecutionPort) {
        this.missionExecutionPort = missionExecutionPort;
    }

    @GetMapping("/mes")
    public Flux<MissionExecutionResponse> mesMissions(@AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.mesMissions(actor.delegationToken()).map(MissionExecutionResponse::from);
    }

    @GetMapping("/{missionId}")
    public Mono<MissionExecutionDetailResponse> chronologie(@PathVariable String missionId,
                                                              @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.chronologie(actor.delegationToken(), missionId).map(MissionExecutionDetailResponse::from);
    }

    @PostMapping("/{missionId}/etapes")
    public Mono<MissionExecutionDetailResponse> ajouterEtape(@PathVariable String missionId,
                                                               @Valid @RequestBody AjouterEtapeRequest request,
                                                               @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.ajouterEtape(actor.delegationToken(), missionId, request.type(), request.libelle(),
                        request.horodatageCapture())
                .map(MissionExecutionDetailResponse::from);
    }
}
