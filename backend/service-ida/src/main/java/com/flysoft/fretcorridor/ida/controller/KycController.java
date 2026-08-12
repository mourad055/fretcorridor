package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.KycDto;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final JwtService jwtService;

    // ── PUT /api/kyc/profil/particulier ───────────────────────
    @PutMapping("/profil/particulier")
    public ResponseEntity<?> completerParticulier(
            @Valid @RequestBody KycDto.CompleterProfilParticulierRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.ok(kycService.completerProfilParticulier(acteurId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── PUT /api/kyc/profil/entreprise ────────────────────────
    @PutMapping("/profil/entreprise")
    public ResponseEntity<?> completerEntreprise(
            @Valid @RequestBody KycDto.CompleterProfilEntrepriseRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.ok(kycService.completerProfilEntreprise(acteurId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/kyc/profil ────────────────────────────────────
    @GetMapping("/profil")
    public ResponseEntity<?> getProfil(@RequestHeader("Authorization") String authHeader) {
        try {
            UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.ok(kycService.getProfil(acteurId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── POST /api/kyc/documents — Dépôt d'une pièce (EF-IDA-03) ─
    @PostMapping("/documents")
    public ResponseEntity<?> deposerDocument(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam("typeDocument") String typeDocument,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.ok(kycService.deposerPiece(acteurId, typeDocument, fichier));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
