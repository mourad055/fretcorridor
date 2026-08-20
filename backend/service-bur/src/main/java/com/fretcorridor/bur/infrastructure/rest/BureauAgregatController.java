package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.AgregationMissionsService;
import com.fretcorridor.bur.infrastructure.rest.dto.AgregatMissionsResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.EnregistrerMissionRequest;
import com.fretcorridor.bur.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Premier endpoint réel de service-bur (Sprint 5, PRD §9). L'ingestion par
 * appel REST direct est un point d'entrée temporaire — elle sera remplacée par
 * une consommation d'événements Kafka une fois le bus câblé pour ce service
 * (cf. domain/MissionRepositoryPort).
 *
 * Bloquant audit CDC §Transverse (constat étendu, 20/08) : tenantId venait
 * du corps/query sans vérification -- vient désormais du JWT forwardé par
 * le gateway, même principe que MissionAppparieeController.
 */
@RestController
@RequestMapping("/api/v1/bur")
public class BureauAgregatController {

    private final AgregationMissionsService agregationMissionsService;
    private final JwtService jwtService;

    public BureauAgregatController(AgregationMissionsService agregationMissionsService, JwtService jwtService) {
        this.agregationMissionsService = agregationMissionsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/missions")
    public ResponseEntity<Void> enregistrerMission(@Valid @RequestBody EnregistrerMissionRequest request,
                                                     @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        agregationMissionsService.enregistrerMission(tenantId, request.axeId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/agregats/missions-par-axe")
    public AgregatMissionsResponse agregatParAxe(@RequestParam String axeId,
                                                  @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return AgregatMissionsResponse.from(agregationMissionsService.agregatPourAxe(tenantId, axeId));
    }
}
