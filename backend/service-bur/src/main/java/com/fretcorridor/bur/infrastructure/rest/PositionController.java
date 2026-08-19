package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.PositionService;
import com.fretcorridor.bur.infrastructure.rest.dto.PositionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vue Bureau des positions, matérialisée depuis Kafka (PositionEtaListener)
 * — jamais un appel direct à service-trk (qui n'expose d'ailleurs aucune API
 * REST). Consommé par le gateway (RG-043/ENF-PRF-02), pas directement par le
 * navigateur — pas de RBAC ici, même choix que MissionAppparieeController.
 */
@RestController
@RequestMapping("/api/v1/bur")
public class PositionController {

    private final PositionService service;

    public PositionController(PositionService service) {
        this.service = service;
    }

    @GetMapping("/positions")
    public List<PositionResponse> positions(@RequestParam String tenantId) {
        return service.listerParTenant(tenantId).stream().map(PositionResponse::from).toList();
    }
}
