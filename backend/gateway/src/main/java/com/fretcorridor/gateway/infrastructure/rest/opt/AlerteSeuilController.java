package com.fretcorridor.gateway.infrastructure.rest.opt;

import com.fretcorridor.gateway.domain.opt.OptPort;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.AlerteSeuilResponse;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.ConfigurerAlerteRequest;
import com.fretcorridor.gateway.infrastructure.rest.opt.dto.EtatAlerteResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent Bureau.
 * {@code acteurId}/{@code tenantId} viennent toujours du JWT, jamais du
 * corps de la requête (ENF-MUL-01, cohérent avec le reste du gateway).
 * Évaluation à la demande uniquement — aucune notification poussée,
 * service-not exige un endpoint côté Mobile qui n'existe pas encore
 * (cf. AlerteSeuilService, Javadoc côté service-bur).
 */
@RestController
@RequestMapping("/api/v1/bureau/alertes")
public class AlerteSeuilController {

    private final OptPort optPort;

    public AlerteSeuilController(OptPort optPort) {
        this.optPort = optPort;
    }

    @PostMapping
    public Mono<ResponseEntity<AlerteSeuilResponse>> configurer(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @Valid @RequestBody ConfigurerAlerteRequest request) {
        return optPort.configurerAlerte(actor.tenantId(), request.axeId(), request.indicateur(),
                        request.comparateur(), request.seuil(), actor.actorId())
                .map(alerte -> ResponseEntity.status(201).body(AlerteSeuilResponse.from(alerte)));
    }

    @GetMapping
    public Mono<List<AlerteSeuilResponse>> lister(@AuthenticationPrincipal AuthenticatedActor actor) {
        return optPort.listerAlertes(actor.tenantId()).map(AlerteSeuilResponse::from).collectList();
    }

    @GetMapping("/etat")
    public Mono<List<EtatAlerteResponse>> etat(@AuthenticationPrincipal AuthenticatedActor actor) {
        return optPort.etatAlertes(actor.tenantId()).map(EtatAlerteResponse::from).collectList();
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> supprimer(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable String id) {
        return optPort.supprimerAlerte(id, actor.tenantId()).thenReturn(ResponseEntity.noContent().build());
    }
}
