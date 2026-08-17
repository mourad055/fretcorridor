package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.AlerteSeuil;
import com.fretcorridor.bur.domain.AlerteSeuilService;
import com.fretcorridor.bur.infrastructure.rest.dto.AlerteSeuilResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.ConfigurerAlerteRequest;
import com.fretcorridor.bur.infrastructure.rest.dto.EtatAlerteResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent. Consommé
 * par le gateway, pas directement par le navigateur — pas de RBAC ici, même
 * convention que MissionAppparieeController (ENF-MUL-01 appliqué côté
 * gateway).
 */
@RestController
@RequestMapping("/api/v1/bur/alertes")
public class AlerteSeuilController {

    private final AlerteSeuilService service;

    public AlerteSeuilController(AlerteSeuilService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AlerteSeuilResponse> configurer(@Valid @RequestBody ConfigurerAlerteRequest request) {
        AlerteSeuil alerte = service.configurer(request.tenantId(), request.axeId(), request.indicateur(),
                request.comparateur(), request.seuil(), request.acteurId());
        return ResponseEntity.status(201).body(AlerteSeuilResponse.from(alerte));
    }

    @GetMapping
    public List<AlerteSeuilResponse> lister(@RequestParam String tenantId) {
        return service.lister(tenantId).stream().map(AlerteSeuilResponse::from).toList();
    }

    @GetMapping("/etat")
    public List<EtatAlerteResponse> etat(@RequestParam String tenantId) {
        return service.evaluer(tenantId).stream().map(EtatAlerteResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id, @RequestParam String tenantId) {
        service.supprimer(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
