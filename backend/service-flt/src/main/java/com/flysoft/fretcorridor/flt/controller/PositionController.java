package com.flysoft.fretcorridor.flt.controller;

import com.flysoft.fretcorridor.flt.dto.PositionDto;
import com.flysoft.fretcorridor.flt.security.JwtService;
import com.flysoft.fretcorridor.flt.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;
    private final JwtService jwtService;

    // Sera appelée par l'app Chauffeur/Transporteur (S6 côté chauffeur, à venir)
    @PostMapping
    public ResponseEntity<?> envoyer(
            @Valid @RequestBody PositionDto.EnvoyerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            positionService.enregistrer(request, tenantId, authHeader);
            return ResponseEntity.status(201).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Consommée par l'app Client (S6) — 204 si aucune position pour l'instant.
    // Pas de filtre par tenant ici : le chargeur qui consulte le suivi et le
    // transporteur qui a affrété la mission peuvent appartenir à des tenants
    // différents (marketplace), cf. commentaire PositionService.getDernierePosition.
    @GetMapping("/mission/{missionId}/derniere")
    public ResponseEntity<?> getDerniere(
            @PathVariable UUID missionId, @RequestHeader("Authorization") String authHeader) {
        try {
            return positionService.getDernierePosition(missionId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
