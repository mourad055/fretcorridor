package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.TransporteurLibelleDto;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.TransporteurLibelleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Libellés d'affichage des transporteurs/chauffeurs (EF-BUR / FE-TRP) —
 * ENF-MUL-01 : uniquement les acteurs du tenant du JWT demandeur.
 */
@RestController
@RequestMapping("/api/ida/transporteurs")
@RequiredArgsConstructor
public class TransporteurLibelleController {

    private final TransporteurLibelleService service;
    private final JwtService jwtService;

    @GetMapping("/libelles")
    public ResponseEntity<List<TransporteurLibelleDto>> libelles(
            @RequestParam List<UUID> ids,
            @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.ok(service.libellesPourTenant(tenantId, ids));
    }
}
