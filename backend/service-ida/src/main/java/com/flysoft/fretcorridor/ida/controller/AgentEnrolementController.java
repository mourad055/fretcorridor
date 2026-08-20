package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.EnrolementDto;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.AgentEnrolementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** UC-IDA-03 : enrôlement assisté par agent de terrain (EF-IDA-06). */
@RestController
@RequestMapping("/api/agent/enrolements")
@RequiredArgsConstructor
public class AgentEnrolementController {

    private final AgentEnrolementService agentEnrolementService;
    private final JwtService jwtService;

    // ── POST /api/agent/enrolements — initier (envoie l'OTP par SMS) ─
    @PostMapping
    public ResponseEntity<?> initier(
            @Valid @RequestBody EnrolementDto.InitierRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID agentUserId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.status(201).body(agentEnrolementService.initier(agentUserId, request));
        } catch (RuntimeException e) {
            if ("ACCES_REFUSE".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── POST /api/agent/enrolements/{id}/activation — OTP + PIN choisi par la personne ─
    @PostMapping("/{id}/activation")
    public ResponseEntity<?> activer(
            @PathVariable UUID id,
            @Valid @RequestBody EnrolementDto.ActiverRequest request) {
        try {
            return ResponseEntity.ok(agentEnrolementService.activer(id, request));
        } catch (RuntimeException e) {
            if ("ENROLEMENT_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
