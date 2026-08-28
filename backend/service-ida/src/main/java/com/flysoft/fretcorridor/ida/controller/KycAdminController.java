package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.KycAdminDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.KycAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Revue KYC par un Admin (backoffice web) — réservé au rôle ADMINISTRATION,
 * même principe inline que CompteAdminController.
 */
@RestController
@RequestMapping("/api/ida/admin/kyc")
@RequiredArgsConstructor
public class KycAdminController {

    private final KycAdminService kycAdminService;
    private final JwtService jwtService;

    @GetMapping("/pending")
    public ResponseEntity<?> listerEnAttente(@RequestParam String tenantId,
                                              @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        List<KycAdminDto.ActeurSummary> dossiers = kycAdminService.listerEnAttente(tenantId);
        return ResponseEntity.ok(dossiers);
    }

    @GetMapping
    public ResponseEntity<?> listerParNiveau(@RequestParam String tenantId,
                                              @RequestParam String niveau,
                                              @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            Acteur.NiveauKyc niveauKyc = Acteur.NiveauKyc.valueOf(niveau);
            List<KycAdminDto.ActeurSummary> dossiers = kycAdminService.listerParNiveau(tenantId, niveauKyc);
            return ResponseEntity.ok(dossiers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("NIVEAU_KYC_INVALIDE");
        } catch (RuntimeException e) {
            if ("NIVEAU_KYC_INVALIDE".equals(e.getMessage())) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{acteurId}")
    public ResponseEntity<?> getDetail(@PathVariable UUID acteurId,
                                        @RequestParam String tenantId,
                                        @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            return ResponseEntity.ok(kycAdminService.getDetail(acteurId, tenantId));
        } catch (RuntimeException e) {
            if ("KYC_ACTEUR_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{acteurId}/pieces/{pieceId}/content")
    public ResponseEntity<?> lirePiece(@PathVariable UUID acteurId,
                                       @PathVariable UUID pieceId,
                                       @RequestParam String tenantId,
                                       @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            var contenu = kycAdminService.lirePiece(acteurId, pieceId, tenantId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contenu.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + contenu.nomFichier() + "\"")
                    .body(contenu.donnees());
        } catch (RuntimeException e) {
            if ("KYC_ACTEUR_INTROUVABLE".equals(e.getMessage()) || "KYC_PIECE_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            if ("KYC_PIECE_ILLISIBLE".equals(e.getMessage())) {
                return ResponseEntity.status(503).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{acteurId}/decision")
    public ResponseEntity<?> prendreDecision(@PathVariable UUID acteurId,
                                              @Valid @RequestBody KycAdminDto.DecisionRequest request,
                                              @RequestParam String tenantId,
                                              @RequestHeader("Authorization") String authHeader) {
        if (!estAdministration(authHeader)) {
            return ResponseEntity.status(403).body("ROLE_ADMINISTRATION_REQUIS");
        }
        try {
            KycAdminDto.ActeurSummary resultat = kycAdminService.prendreDecision(acteurId, tenantId, request);
            return ResponseEntity.ok(resultat);
        } catch (RuntimeException e) {
            if ("KYC_ACTEUR_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            if ("DECISION_INVALIDE".equals(e.getMessage())) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean estAdministration(String authHeader) {
        return jwtService.extraireRoles(authHeader.substring(7)).contains("ADMINISTRATION");
    }
}
