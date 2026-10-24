package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.ConfigurerTarifChauffeur;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurPort;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.ConfigurerTarifChauffeurRequest;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.IntervalleTarifResponse;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.ModifierTarifChauffeurRequest;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.TarifChauffeurResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tarification par chauffeur (plan §1bis, PR #145 côté service-cap).
 * "Mes tarifs" (JWT chauffeur/transporteur) + intervalle public consommé par
 * l'app Client pour l'écran Estimation (aucune donnée personnelle exposée).
 */
@RestController
public class TarifChauffeurController {

    private final TarifChauffeurPort tarifChauffeurPort;

    public TarifChauffeurController(TarifChauffeurPort tarifChauffeurPort) {
        this.tarifChauffeurPort = tarifChauffeurPort;
    }

    @PostMapping("/api/v1/mes-tarifs")
    public Mono<ResponseEntity<TarifChauffeurResponse>> configurer(@Valid @RequestBody ConfigurerTarifChauffeurRequest request,
                                                                     @AuthenticationPrincipal AuthenticatedActor actor) {
        var requete = new ConfigurerTarifChauffeur(request.axeId(), request.categorieMarchandise(), request.tarifFcfaParKg());
        return tarifChauffeurPort.configurer(requete, actor.delegationToken())
                .map(t -> ResponseEntity.status(HttpStatus.CREATED).body(TarifChauffeurResponse.from(t)));
    }

    @GetMapping("/api/v1/mes-tarifs")
    public Flux<TarifChauffeurResponse> mesTarifs(@AuthenticationPrincipal AuthenticatedActor actor) {
        return tarifChauffeurPort.mesTarifs(actor.delegationToken()).map(TarifChauffeurResponse::from);
    }

    @PatchMapping("/api/v1/mes-tarifs/{id}")
    public Mono<TarifChauffeurResponse> modifier(@PathVariable String id, @Valid @RequestBody ModifierTarifChauffeurRequest request,
                                                  @AuthenticationPrincipal AuthenticatedActor actor) {
        return tarifChauffeurPort.modifier(id, request.tarifFcfaParKg(), actor.delegationToken())
                .map(TarifChauffeurResponse::from);
    }

    @PatchMapping("/api/v1/mes-tarifs/{id}/actif")
    public Mono<TarifChauffeurResponse> toggleActif(@PathVariable String id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return tarifChauffeurPort.toggleActif(id, actor.delegationToken()).map(TarifChauffeurResponse::from);
    }

    @DeleteMapping("/api/v1/mes-tarifs/{id}")
    public Mono<ResponseEntity<Void>> supprimer(@PathVariable String id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return tarifChauffeurPort.supprimer(id, actor.delegationToken())
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @GetMapping("/api/v1/tarifs/intervalle")
    public Mono<IntervalleTarifResponse> intervalle(@RequestParam String axeId, @RequestParam String categorie) {
        return tarifChauffeurPort.intervalle(axeId, categorie).map(IntervalleTarifResponse::from);
    }
}
