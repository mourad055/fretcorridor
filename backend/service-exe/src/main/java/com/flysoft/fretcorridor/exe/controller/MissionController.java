package com.flysoft.fretcorridor.exe.controller;

import com.flysoft.fretcorridor.exe.security.JwtService;
import com.flysoft.fretcorridor.exe.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final JwtService jwtService;

    // UC-EXE-01 côté Client — 204 si aucune mission encore créée (matching pas encore actif)
    @GetMapping("/demande/{demandeId}/chronologie")
    public ResponseEntity<?> getChronologie(
            @PathVariable UUID demandeId, @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return missionService.getChronologiePourDemande(demandeId, tenantId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
