package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.AlerteSeuil;
import com.fretcorridor.bur.domain.AlerteSeuilService;
import com.fretcorridor.bur.infrastructure.rest.dto.AlerteSeuilResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.ConfigurerAlerteRequest;
import com.fretcorridor.bur.infrastructure.rest.dto.EtatAlerteResponse;
import com.fretcorridor.bur.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent. Consommé
 * par le gateway, pas directement par le navigateur.
 *
 * Bloquant audit CDC §Transverse (constat étendu, 20/08) : tenantId/acteurId
 * venaient jusqu'ici du corps/query sans vérification -- un acteur d'un
 * tenant pouvait configurer/consulter/supprimer les alertes d'un autre
 * tenant en atteignant ce port directement (le gateway filtre bien côté
 * client, mais ne l'imposait pas ici). Ces deux identités viennent
 * désormais du JWT forwardé par le gateway (même secret partagé
 * service-ida), même principe que DossierController côté service-adm.
 */
@RestController
@RequestMapping("/api/v1/bur/alertes")
public class AlerteSeuilController {

    private final AlerteSeuilService service;
    private final JwtService jwtService;

    public AlerteSeuilController(AlerteSeuilService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<AlerteSeuilResponse> configurer(@Valid @RequestBody ConfigurerAlerteRequest request,
                                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        AlerteSeuil alerte = service.configurer(jwtService.extraireTenantId(token), request.axeId(),
                request.indicateur(), request.comparateur(), request.seuil(), jwtService.extraireActeurId(token));
        return ResponseEntity.status(201).body(AlerteSeuilResponse.from(alerte));
    }

    @GetMapping
    public List<AlerteSeuilResponse> lister(@RequestHeader("Authorization") String authHeader) {
        return service.lister(jwtService.extraireTenantId(authHeader.substring(7))).stream()
                .map(AlerteSeuilResponse::from).toList();
    }

    @GetMapping("/etat")
    public List<EtatAlerteResponse> etat(@RequestHeader("Authorization") String authHeader) {
        return service.evaluer(jwtService.extraireTenantId(authHeader.substring(7))).stream()
                .map(EtatAlerteResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        service.supprimer(id, jwtService.extraireTenantId(authHeader.substring(7)));
        return ResponseEntity.noContent().build();
    }
}
