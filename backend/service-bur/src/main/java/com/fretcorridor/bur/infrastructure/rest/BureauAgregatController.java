package com.fretcorridor.bur.infrastructure.rest;

import com.fretcorridor.bur.domain.AgregationMissionsService;
import com.fretcorridor.bur.infrastructure.rest.dto.AgregatMissionsResponse;
import com.fretcorridor.bur.infrastructure.rest.dto.EnregistrerMissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Premier endpoint réel de service-bur (Sprint 5, PRD §9). L'ingestion par
 * appel REST direct est un point d'entrée temporaire — elle sera remplacée par
 * une consommation d'événements Kafka une fois le bus câblé pour ce service
 * (cf. domain/MissionRepositoryPort).
 */
@RestController
@RequestMapping("/api/v1/bur")
public class BureauAgregatController {

    private final AgregationMissionsService agregationMissionsService;

    public BureauAgregatController(AgregationMissionsService agregationMissionsService) {
        this.agregationMissionsService = agregationMissionsService;
    }

    @PostMapping("/missions")
    public ResponseEntity<Void> enregistrerMission(@Valid @RequestBody EnregistrerMissionRequest request) {
        agregationMissionsService.enregistrerMission(request.tenantId(), request.axeId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/agregats/missions-par-axe")
    public AgregatMissionsResponse agregatParAxe(
            @RequestParam String tenantId,
            @RequestParam String axeId
    ) {
        return AgregatMissionsResponse.from(agregationMissionsService.agregatPourAxe(tenantId, axeId));
    }
}
