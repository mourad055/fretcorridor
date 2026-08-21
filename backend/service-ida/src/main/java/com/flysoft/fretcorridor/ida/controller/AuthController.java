package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/login ────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // ── POST /api/auth/inscription-chargeur ─────────────────
    // EF-MKT-01 : inscription légère (S1) — le profil complet vient au S2 (KYC)
    @PostMapping("/inscription-chargeur")
    public ResponseEntity<?> inscrireChargeur(@Valid @RequestBody AuthDto.InscriptionChargeurRequest request) {
        try {
            return ResponseEntity.status(201).body(authService.inscrireChargeur(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── POST /api/auth/inscription-transporteur ──────────────
    @PostMapping("/inscription-transporteur")
    public ResponseEntity<?> inscrireTransporteur(@Valid @RequestBody AuthDto.InscriptionTransporteurRequest request) {
        try {
            return ResponseEntity.status(201).body(authService.inscrireTransporteur(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── POST /api/auth/refresh ───────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody java.util.Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.rafraichir(body.get("refreshToken")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
