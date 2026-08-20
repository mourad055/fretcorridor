package com.fretcorridor.gateway.infrastructure.rest.opt;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.DefinirEstimationMarcheRequest;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.MissionAppparieeCsvExporter;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.MissionAppparieeResponse;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.ObservatoireAxeResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * FE-BUR-01 (Sprint 5, missions appariées) : un Bureau ne voit que les missions
 * de son propre tenant — le tenantId vient exclusivement du JWT (ENF-MUL-01).
 * EF-BUR-02 : filtrage, détail et export des flux supervisés.
 */
@RestController
public class MissionAppparieeController {

    private final OptPort optPort;
    private final AdmPort admPort;

    public MissionAppparieeController(OptPort optPort, AdmPort admPort) {
        this.optPort = optPort;
        this.admPort = admPort;
    }

    @GetMapping("/api/v1/bureau/missions-appariees")
    public Flux<MissionAppparieeResponse> missionsAppariees(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestParam(required = false) StatutMission statut,
            @RequestParam(required = false) String axeId) {
        return missionsFiltrees(actor, statut, axeId).map(MissionAppparieeResponse::from);
    }

    /** EF-BUR-06 : consultation du détail d'une mission (donnée individuelle, CDC UC-BUR-01 étape 4) journalisée. */
    @GetMapping("/api/v1/bureau/missions-appariees/{missionId}")
    public Mono<ResponseEntity<MissionAppparieeResponse>> detail(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @PathVariable String missionId) {
        return admPort.enregistrerAudit(actor.tenantId(), actor.actorId(), "CONSULTATION_MISSION_DETAIL", "mission:" + missionId)
                .then(optPort.listerMissionsParTenant(actor.tenantId(), actor.delegationToken())
                        .filter(mission -> mission.id().equals(missionId))
                        .next()
                        .map(mission -> ResponseEntity.ok(MissionAppparieeResponse.from(mission)))
                        .defaultIfEmpty(ResponseEntity.notFound().build()));
    }

    /** EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe — agrégat anonymisé, pas de journalisation EF-BUR-06 (RG-086). */
    @GetMapping("/api/v1/bureau/observatoire/{axeId}")
    public Mono<ObservatoireAxeResponse> observatoire(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @PathVariable String axeId) {
        return optPort.observatoirePourAxe(actor.tenantId(), axeId, actor.delegationToken()).map(ObservatoireAxeResponse::from);
    }

    /** EF-BUR-05, RG-087 : un agent Bureau déclare l'estimation de marché d'un axe — acteurId/tenantId toujours du JWT (ENF-MUL-01). */
    @PutMapping("/api/v1/bureau/observatoire/{axeId}/estimation-marche")
    public Mono<ResponseEntity<Void>> definirEstimationMarche(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @PathVariable String axeId,
            @Valid @RequestBody DefinirEstimationMarcheRequest request) {
        return optPort.definirEstimationMarche(actor.tenantId(), axeId, request.volumeMensuelEstime(),
                        request.source(), actor.actorId(), actor.delegationToken())
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @GetMapping("/api/v1/bureau/missions-appariees/export")
    public Mono<ResponseEntity<String>> exporter(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestParam(required = false) StatutMission statut,
            @RequestParam(required = false) String axeId) {
        return missionsFiltrees(actor, statut, axeId)
                .collectList()
                .map(missions -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"missions-supervisees.csv\"")
                        .body(MissionAppparieeCsvExporter.versCsv(missions)));
    }

    private Flux<MissionAppariee> missionsFiltrees(AuthenticatedActor actor, StatutMission statut, String axeId) {
        return optPort.listerMissionsParTenant(actor.tenantId(), actor.delegationToken())
                .filter(mission -> statut == null || mission.statut() == statut)
                .filter(mission -> axeId == null || axeId.isBlank() || mission.axeId().equals(axeId));
    }
}
