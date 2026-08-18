package com.flysoft.fretcorridor.exe.controller;

import com.flysoft.fretcorridor.exe.dto.MissionDto;
import com.flysoft.fretcorridor.exe.security.JwtService;
import com.flysoft.fretcorridor.exe.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    // S7 côté Chauffeur/Transporteur — cf. AffectationConfirmeeListener :
    // reste vide tant que transporteurId n'est pas peuplé en amont
    // (écart documenté, service-opt).
    @GetMapping("/mes")
    public ResponseEntity<List<MissionDto.MissionResumeResponse>> mesMissions(
            @RequestHeader("Authorization") String authHeader) {
        UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.ok(missionService.listerMesMissions(transporteurId, tenantId));
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<?> getMission(
            @PathVariable UUID missionId, @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return ResponseEntity.ok(missionService.getChronologie(missionId, transporteurId, tenantId));
        } catch (RuntimeException e) {
            if ("MISSION_INTROUVABLE".equals(e.getMessage()) || "ACCES_REFUSE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // EF-EXE-02/04 : le chauffeur avance sa mission (prise en charge, en
    // transit, livraison) ou journalise un incident.
    @PostMapping("/{missionId}/etapes")
    public ResponseEntity<?> ajouterEtape(
            @PathVariable UUID missionId, @Valid @RequestBody MissionDto.AjouterEtapeRequest requete,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            return ResponseEntity.ok(missionService.ajouterEtape(missionId, transporteurId, tenantId, requete));
        } catch (RuntimeException e) {
            if ("MISSION_INTROUVABLE".equals(e.getMessage()) || "ACCES_REFUSE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
