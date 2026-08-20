package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.DecisionRequest;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.DossierConsolideResponse;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.DossierResponse;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionResponse;
import com.fretcorridor.gateway.infrastructure.rest.pay.dto.EcritureVueResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** FE-ADM-01/02 : file de travail priorisée et dossier consolidé (Sprint 10). */
@RestController
@RequestMapping("/api/v1/admin/dossiers")
public class DossierController {

    private final AdmPort admPort;
    private final ExePort exePort;
    private final PayReadPort payReadPort;

    public DossierController(AdmPort admPort, ExePort exePort, PayReadPort payReadPort) {
        this.admPort = admPort;
        this.exePort = exePort;
        this.payReadPort = payReadPort;
    }

    @GetMapping
    public Flux<DossierResponse> fileDeTravail(@RequestParam String tenantId,
                                                @AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.fileDeTravail(tenantId, actor.delegationToken()).map(DossierResponse::from);
    }

    /** ENF-SEC-02 : consultation d'un dossier (donnée individuelle) journalisée, même principe que MissionAppparieeController.detail. */
    @GetMapping("/{dossierId}")
    public Mono<DossierConsolideResponse> consolide(@PathVariable String dossierId,
                                                      @AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.enregistrerAudit(actor.tenantId(), actor.actorId(), "CONSULTATION_DOSSIER_DETAIL",
                        "dossier:" + dossierId, actor.delegationToken())
                .then(consulterDossier(dossierId, actor));
    }

    private Mono<DossierConsolideResponse> consulterDossier(String dossierId, AuthenticatedActor actor) {
        return admPort.dossier(dossierId, actor.delegationToken()).flatMap(dossier -> {
            DossierResponse dossierResponse = DossierResponse.from(dossier);
            if (dossier.missionId() == null) {
                return Mono.just(new DossierConsolideResponse(dossierResponse, null, java.util.List.of()));
            }
            Mono<java.util.List<MissionResponse>> mission = exePort.listerMissionsParTenant(dossier.tenantId())
                    .filter(m -> m.id().equals(dossier.missionId()))
                    .map(MissionResponse::from)
                    .collectList();
            Mono<java.util.List<EcritureVueResponse>> ecritures = payReadPort.rapportDuTenant(dossier.tenantId(), actor.delegationToken())
                    .filter(e -> dossier.missionId().equals(e.missionId()))
                    .map(EcritureVueResponse::from)
                    .collectList();
            return Mono.zip(mission, ecritures)
                    .map(tuple -> new DossierConsolideResponse(dossierResponse,
                            tuple.getT1().isEmpty() ? null : tuple.getT1().get(0), tuple.getT2()));
        });
    }

    @PostMapping("/{dossierId}/prise-en-charge")
    public Mono<DossierResponse> priseEnCharge(@PathVariable String dossierId,
                                                @AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.priseEnCharge(dossierId, actor.actorId(), actor.delegationToken()).map(DossierResponse::from);
    }

    @PostMapping("/{dossierId}/decision")
    public Mono<DossierResponse> decider(@PathVariable String dossierId, @Valid @RequestBody DecisionRequest request,
                                          @AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.decider(dossierId, request.decision(), request.motif(), actor.actorId(), actor.delegationToken())
                .map(DossierResponse::from);
    }

    @PostMapping("/escalade")
    public Flux<DossierResponse> escalader(@AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.declencherEscalade(actor.delegationToken()).map(DossierResponse::from);
    }
}
