package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.CompteAdminDto;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.GestionCompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1). Réservé au rôle
 * ADMINISTRATION — même principe inline que AffiliationController (rôle
 * requis vérifié depuis le JWT, pas seulement délégué au gateway).
 */
@RestController
@RequestMapping("/api/ida/comptes")
@RequiredArgsConstructor
public class CompteAdminController {

    private final GestionCompteService gestionCompteService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<?> lister(@RequestParam String tenantId,
                                     @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        List<CompteAdminDto.CompteResponse> comptes = gestionCompteService.listerParTenant(tenantId).stream()
                .map(CompteAdminDto.CompteResponse::from)
                .toList();
        return ResponseEntity.ok(comptes);
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@PathVariable UUID id,
                                            @Valid @RequestBody CompteAdminDto.ChangerStatutRequest request,
                                            @RequestParam String tenantId,
                                            @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            var compte = gestionCompteService.changerStatut(id, tenantId, request.getActif());
            return ResponseEntity.ok(CompteAdminDto.CompteResponse.from(compte));
        } catch (RuntimeException e) {
            if ("COMPTE_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<?> changerRoles(@PathVariable UUID id,
                                           @Valid @RequestBody CompteAdminDto.ChangerRolesRequest request,
                                           @RequestParam String tenantId,
                                           @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            var compte = gestionCompteService.changerRoles(id, tenantId, request.getRoles());
            return ResponseEntity.ok(CompteAdminDto.CompteResponse.from(compte));
        } catch (RuntimeException e) {
            if ("COMPTE_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean estAdministration(String authHeader) {
        return jwtService.extraireRoles(authHeader.substring(7)).contains("ADMINISTRATION");
    }
}
