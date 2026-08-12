package com.flysoft.fretcorridor.flt.controller;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.security.JwtService;
import com.flysoft.fretcorridor.flt.service.VehiculeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** S10 : console de flotte simplifiée (mode transporteur étendu). */
@RestController
@RequestMapping("/api/flt/vehicules")
@RequiredArgsConstructor
public class VehiculeController {

    private final VehiculeService vehiculeService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<VehiculeDto.VehiculeResponse> declarer(
            @Valid @RequestBody VehiculeDto.DeclarerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.status(201).body(vehiculeService.declarer(acteurId, tenantId, request));
    }

    @GetMapping("/mes")
    public ResponseEntity<?> mesVehicules(@RequestHeader("Authorization") String authHeader) {
        UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.ok(vehiculeService.listerMesVehicules(acteurId, tenantId));
    }
}
