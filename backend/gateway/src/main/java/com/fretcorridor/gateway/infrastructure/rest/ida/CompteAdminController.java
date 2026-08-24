package com.fretcorridor.gateway.infrastructure.rest.ida;

import com.fretcorridor.gateway.domain.ida.IdaCompteAdminPort;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.ChangerRolesRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.ChangerStatutRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.CompteAdminResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1) — appelle service-ida
 * (Mobile), source d'identité unique. Réservé ADMIN par la config Security
 * globale (`/api/v1/admin/**`), même règle que les autres écrans Admin.
 */
@RestController
@RequestMapping("/api/v1/admin/comptes")
public class CompteAdminController {

    private final IdaCompteAdminPort idaCompteAdminPort;

    public CompteAdminController(IdaCompteAdminPort idaCompteAdminPort) {
        this.idaCompteAdminPort = idaCompteAdminPort;
    }

    @GetMapping
    public Flux<CompteAdminResponse> lister(@RequestParam String tenantId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaCompteAdminPort.listerParTenant(tenantId, actor.delegationToken()).map(CompteAdminResponse::from);
    }

    @PutMapping("/{id}/statut")
    public Mono<CompteAdminResponse> changerStatut(@PathVariable String id, @RequestParam String tenantId,
                                                     @Valid @RequestBody ChangerStatutRequest request,
                                                     @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaCompteAdminPort.changerStatut(id, tenantId, request.actif(), actor.delegationToken())
                .map(CompteAdminResponse::from);
    }

    @PutMapping("/{id}/roles")
    public Mono<CompteAdminResponse> changerRoles(@PathVariable String id, @RequestParam String tenantId,
                                                    @Valid @RequestBody ChangerRolesRequest request,
                                                    @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaCompteAdminPort.changerRoles(id, tenantId, request.roles(), actor.delegationToken())
                .map(CompteAdminResponse::from);
    }
}
