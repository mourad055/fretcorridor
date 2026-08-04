package com.fretcorridor.gateway.infrastructure.rest.kyc;

import com.fretcorridor.gateway.domain.kyc.KycPort;
import com.fretcorridor.gateway.infrastructure.audit.AuditLog;
import com.fretcorridor.gateway.infrastructure.rest.kyc.dto.DecisionRequest;
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
 * FE-ADM-06 (Sprint 2) : dashboard de validation KYC. Toute décision est journalisée
 * (ENF-SEC-02) — l'action porte l'identité de l'opérateur, jamais anonyme.
 */
@RestController
@RequestMapping("/api/v1/admin/kyc")
public class KycController {

    private final KycPort kycPort;
    private final AuditLog auditLog;

    public KycController(KycPort kycPort, AuditLog auditLog) {
        this.kycPort = kycPort;
        this.auditLog = auditLog;
    }

    @GetMapping("/pending")
    public Flux<KycDossierResponse> pending() {
        return kycPort.listerEnAttente().map(KycDossierResponse::from);
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

        return kycPort.decider(dossierId, request.decision(), idempotencyKey)
                .doOnNext(dossier -> auditLog.enregistrer(
                        actor.actorId(),
                        "KYC_DECISION_" + request.decision(),
                        "kyc-dossier:" + dossierId))
                .map(dossier -> ResponseEntity.ok(KycDossierResponse.from(dossier)));
    }

    public static class MissingIdempotencyKeyException extends RuntimeException {
        MissingIdempotencyKeyException() {
            super("L'en-tête X-Idempotency-Key est obligatoire sur cette mutation");
        }
    }
}
