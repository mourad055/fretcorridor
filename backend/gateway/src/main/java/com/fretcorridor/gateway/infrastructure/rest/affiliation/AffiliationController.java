package com.fretcorridor.gateway.infrastructure.rest.affiliation;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.affiliation.AffiliationPort;
import com.fretcorridor.gateway.domain.affiliation.TenantOption;
import com.fretcorridor.gateway.infrastructure.rest.dto.LoginResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import com.fretcorridor.gateway.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * S18 (Sprint 18, "Second tenant institutionnel", audit de suivi 23 aout) :
 * "Sélection de tenant au login (si multi-bureau)" — Plan d'exécution.
 * Route accessible à tout acteur authentifié (mesTenants/selection ne
 * révèlent/n'accordent jamais rien au-delà de ses propres affiliations,
 * déjà vérifiées côté service-ida) — pas de restriction de rôle ici,
 * contrairement à AffiliationBureauController (invitation).
 */
@RestController
@RequestMapping("/api/v1/auth/tenants")
public class AffiliationController {

    private final AffiliationPort affiliationPort;
    private final JwtService jwtService;

    public AffiliationController(AffiliationPort affiliationPort, JwtService jwtService) {
        this.affiliationPort = affiliationPort;
        this.jwtService = jwtService;
    }

    public record SelectionRequest(@NotBlank String tenantId) {
    }

    @GetMapping
    public Mono<List<TenantOption>> mesTenants(@AuthenticationPrincipal AuthenticatedActor actor) {
        return affiliationPort.mesTenants(actor.delegationToken());
    }

    @PostMapping("/selection")
    public Mono<LoginResponse> selectionner(@Valid @RequestBody SelectionRequest request,
                                             @AuthenticationPrincipal AuthenticatedActor actor) {
        return affiliationPort
                .selectionner(actor.delegationToken(), actor.phone(), actor.role(), request.tenantId())
                .map(this::toResponse);
    }

    private LoginResponse toResponse(Actor actor) {
        String token = jwtService.issue(actor);
        return new LoginResponse(token, actor.role().name(), actor.tenantId());
    }
}
