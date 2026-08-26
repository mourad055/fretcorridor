package com.fretcorridor.gateway.infrastructure.rest.kyc;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.kyc.KycPort;
import com.fretcorridor.gateway.infrastructure.rest.kyc.dto.DecisionRequest;
import com.fretcorridor.gateway.infrastructure.rest.kyc.dto.KycDetailResponse;
import com.fretcorridor.gateway.infrastructure.rest.kyc.dto.KycDossierResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * FE-ADM-06 : dashboard de validation KYC. Miroir additif de
 * /api/ida/admin/kyc — toute décision est journalisée (ENF-SEC-02).
 */
@RestController
@RequestMapping("/api/v1/admin/kyc")
public class KycController {

    private final KycPort kycPort;
    private final AdmPort admPort;

    public KycController(KycPort kycPort, AdmPort admPort) {
        this.kycPort = kycPort;
        this.admPort = admPort;
    }

    @GetMapping("/pending")
    public Flux<KycDossierResponse> pending(@AuthenticationPrincipal AuthenticatedActor actor) {
        return kycPort.listerEnAttente(actor.tenantId(), actor.delegationToken()).map(KycDossierResponse::from);
    }

    @GetMapping
    public Flux<KycDossierResponse> parNiveau(
            @RequestParam String niveau,
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        return kycPort.listerParNiveau(actor.tenantId(), niveau, actor.delegationToken())
                .map(KycDossierResponse::from);
    }

    @GetMapping("/{dossierId}")
    public Mono<KycDetailResponse> detail(
            @PathVariable String dossierId,
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        return kycPort.detail(dossierId, actor.tenantId(), actor.delegationToken())
                .map(KycDetailResponse::from);
    }

    @PostMapping("/{dossierId}/decision")
    public Mono<ResponseEntity<KycDossierResponse>> decide(
            @PathVariable String dossierId,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal AuthenticatedActor actor,
            ServerWebExchange exchange
    ) {
        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new MissingIdempotencyKeyException());
        }

        return kycPort.decider(
                        dossierId,
                        request.decision(),
                        idempotencyKey,
                        actor.tenantId(),
                        actor.delegationToken(),
                        request.motif())
                .flatMap(dossier -> admPort
                        .enregistrerAudit(actor.tenantId(), actor.actorId(), "KYC_DECISION_" + request.decision(),
                                "kyc-dossier:" + dossierId, actor.delegationToken())
                        .thenReturn(dossier))
                .map(dossier -> ResponseEntity.ok(KycDossierResponse.from(dossier)));
    }

    public static class MissingIdempotencyKeyException extends RuntimeException {
        MissingIdempotencyKeyException() {
            super("L'en-tête X-Idempotency-Key est obligatoire sur cette mutation");
        }
    }
}
