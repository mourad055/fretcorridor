package com.flysoft.fretcorridor.flt.controller;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.security.JwtService;
import com.flysoft.fretcorridor.flt.service.VehiculeService;
import com.flysoft.fretcorridor.flt.entity.Vehicule;
import com.flysoft.fretcorridor.flt.repository.VehiculeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** S10 : console de flotte simplifiée (mode transporteur étendu). */
@RestController
@RequestMapping("/api/flt/vehicules")
@RequiredArgsConstructor
public class VehiculeController {

    private final VehiculeService vehiculeService;
    private final JwtService jwtService;
    private final VehiculeRepository vehiculeRepository;

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

    // AJOUT : consomme en synchrone par service-cap (ServiceFltClient, meme
    // porteur Mobile, Plan d'execution S4.3) pour resoudre le proprietaire
    // d'un vehicule au moment de la declaration de capacite - ferme le bug
    // S7. Pas de garde JWT ici : appel interne inter-services (meme porteur),
    // pas un appel client final - a revoir si le gateway impose une
    // authentification systemique meme sur les appels internes.
    @GetMapping("/{id}")
    public ResponseEntity<VehiculeDto.VehiculeResponse> consulter(@PathVariable UUID id) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicule introuvable : " + id));
        return ResponseEntity.ok(VehiculeDto.VehiculeResponse.fromEntity(vehicule));
    }
}
