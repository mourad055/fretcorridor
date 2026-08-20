package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.CreerTenantRequest;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.TenantResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** FE-ADM-04 : gestion des tenants (bureaux de fret, entités multi-tenant). */
@RestController
@RequestMapping("/api/v1/admin/tenants")
public class TenantController {

    private final AdmPort admPort;

    public TenantController(AdmPort admPort) {
        this.admPort = admPort;
    }

    @GetMapping
    public Flux<TenantResponse> lister(@AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.tenants(actor.delegationToken()).map(TenantResponse::from);
    }

    @PostMapping
    public Mono<TenantResponse> creer(@Valid @RequestBody CreerTenantRequest request,
                                       @AuthenticationPrincipal AuthenticatedActor actor) {
        return admPort.creerTenant(request.id(), request.nom(), request.pays(), actor.actorId(), actor.delegationToken())
                .map(TenantResponse::from);
    }
}
