package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.MissionAppparieeService;
import com.fretcorridor.bur.domain.ObservatoireService;
import com.fretcorridor.bur.infrastructure.rest.dto.DefinirEstimationMarcheRequest;
import com.fretcorridor.bur.infrastructure.rest.dto.MissionAppparieeResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.ObservatoireAxeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    private final ObservatoireService observatoireService;

    public MissionAppparieeController(MissionAppparieeService service, ObservatoireService observatoireService) {
        this.service = service;
        this.observatoireService = observatoireService;
    }

    @GetMapping("/missions-appariees")
    public List<MissionAppparieeResponse> missionsAppariees(@RequestParam String tenantId) {
        return service.listerParTenant(tenantId).stream().map(MissionAppparieeResponse::from).toList();
    }

    /** EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe (volumes, prix médian et dispersion, déséquilibre directionnel). */
    @GetMapping("/observatoire")
    public ObservatoireAxeResponse observatoire(@RequestParam String tenantId, @RequestParam UUID axeId) {
        return ObservatoireAxeResponse.from(observatoireService.indicateursPourAxe(tenantId, axeId, Instant.now()));
    }

    /** EF-BUR-05, RG-087 : estimation déclarative du volume mensuel réel du marché d'un axe, saisie par un agent Bureau. */
    @PutMapping("/estimation-marche")
    public ResponseEntity<Void> definirEstimationMarche(@Valid @RequestBody DefinirEstimationMarcheRequest request) {
        observatoireService.definirEstimationMarche(request.tenantId(), request.axeId(), request.volumeMensuelEstime(),
                request.source(), request.acteurId(), Instant.now());
        return ResponseEntity.noContent().build();
    }
}
