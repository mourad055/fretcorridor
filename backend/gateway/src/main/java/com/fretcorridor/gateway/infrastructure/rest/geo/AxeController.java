package com.fretcorridor.gateway.infrastructure.rest.geo;

import com.fretcorridor.gateway.domain.geo.GeoPort;
import com.fretcorridor.gateway.infrastructure.rest.geo.dto.AxeResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * FE-BUR-01 (Sprint 3, version carte) : un Bureau ne voit que les axes de son
 * propre tenant — le tenantId vient exclusivement du JWT (ENF-MUL-01), jamais
 * d'un paramètre de requête.
 */
@RestController
public class AxeController {

    private final GeoPort geoPort;

    public AxeController(GeoPort geoPort) {
        this.geoPort = geoPort;
    }

    @GetMapping("/api/v1/bureau/axes")
    public Flux<AxeResponse> axes(@AuthenticationPrincipal AuthenticatedActor actor) {
        return geoPort.listerAxesParTenant(actor.tenantId()).map(AxeResponse::from);
    }

    /**
     * S3 (app Chauffeur/Transporteur, EF-GEO-03) : même source que le Bureau
     * — les verrous (matching/paiement inactifs) restent visibles, pas
     * masqués, pour que l'acteur comprenne pourquoi un axe est indisponible
     * (RG-012 : ne jamais bloquer la simple consultation).
     */
    @GetMapping("/api/v1/axes")
    public Flux<AxeResponse> axesMobile(@AuthenticationPrincipal AuthenticatedActor actor) {
        return geoPort.listerAxesParTenant(actor.tenantId()).map(AxeResponse::from);
    }
}
