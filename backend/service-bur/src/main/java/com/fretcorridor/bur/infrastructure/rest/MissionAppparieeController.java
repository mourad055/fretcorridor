package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.MissionAppparieeService;
import com.fretcorridor.bur.infrastructure.rest.dto.MissionAppparieeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vue Bureau des missions appariées, matérialisée depuis Kafka
 * (AffectationConfirmeeListener) — jamais un appel direct à service-opt
 * (cf. MissionAppariee, Javadoc). Consommé par le gateway (EF-BUR-01/02),
 * pas directement par le navigateur — pas de RBAC ici, le gateway filtre
 * déjà par tenant avant d'exposer au Bureau (ENF-MUL-01 appliqué côté
 * gateway, cohérent avec le reste des services internes sans sécurité
 * propre, ex. service-geo).
 */
@RestController
@RequestMapping("/api/v1/bur")
public class MissionAppparieeController {

    private final MissionAppparieeService service;

    public MissionAppparieeController(MissionAppparieeService service) {
        this.service = service;
    }

    @GetMapping("/missions-appariees")
    public List<MissionAppparieeResponse> missionsAppariees(@RequestParam String tenantId) {
        return service.listerParTenant(tenantId).stream().map(MissionAppparieeResponse::from).toList();
    }
}
