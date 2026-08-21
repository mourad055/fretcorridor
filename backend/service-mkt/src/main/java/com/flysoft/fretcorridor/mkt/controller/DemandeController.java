package com.flysoft.fretcorridor.mkt.controller;

import com.flysoft.fretcorridor.mkt.client.ReservationCapaciteException;
import com.flysoft.fretcorridor.mkt.dto.DemandeDto;
import com.flysoft.fretcorridor.mkt.security.JwtService;
import com.flysoft.fretcorridor.mkt.service.DemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;
    private final JwtService jwtService;

    // ── POST /api/demandes — publier (UC-MKT-01, RG-038) ──────
    @PostMapping
    public ResponseEntity<?> publier(
            @Valid @RequestBody DemandeDto.PublierRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID acteurId = jwtService.extraireActeurId(token);
            String tenantId = jwtService.extraireTenantId(token);
            String niveauKyc = jwtService.extraireNiveauKyc(token);

            var reponse = demandeService.publier(request, acteurId, tenantId, niveauKyc);
            return ResponseEntity.status(201).body(reponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/demandes/mes-demandes ─────────────────────────
    @GetMapping("/mes-demandes")
    public ResponseEntity<?> getMesDemandes(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID acteurId = jwtService.extraireActeurId(token);
            String tenantId = jwtService.extraireTenantId(token);
            return ResponseEntity.ok(demandeService.getMesDemandes(acteurId, tenantId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/demandes/{id}/propositions — RG-039/EF-MKT-07 ─
    @GetMapping("/{id}/propositions")
    public ResponseEntity<?> getPropositions(
            @PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String tenantId = jwtService.extraireTenantId(token);
            return ResponseEntity.ok(demandeService.getPropositions(id, tenantId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── POST /api/demandes/{id}/propositions/{propositionId}/accepter — RG-039/EF-MKT-08 ─
    @PostMapping("/{id}/propositions/{propositionId}/accepter")
    public ResponseEntity<?> accepterProposition(
            @PathVariable UUID id, @PathVariable UUID propositionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String tenantId = jwtService.extraireTenantId(token);
            return ResponseEntity.ok(demandeService.accepterProposition(id, propositionId, tenantId));
        } catch (ReservationCapaciteException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (RuntimeException e) {
            if ("DEMANDE_INTROUVABLE".equals(e.getMessage()) || "PROPOSITION_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            if ("PROPOSITION_DEJA_TRAITEE".equals(e.getMessage())) {
                return ResponseEntity.status(409).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
