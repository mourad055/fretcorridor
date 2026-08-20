package com.flysoft.fretcorridor.flt.controller;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.security.JwtService;
import com.flysoft.fretcorridor.flt.service.ImmatriculationDejaUtiliseeException;
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
    public ResponseEntity<?> declarer(
            @Valid @RequestBody VehiculeDto.DeclarerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        try {
            return ResponseEntity.status(201).body(vehiculeService.declarer(acteurId, tenantId, request));
        } catch (ImmatriculationDejaUtiliseeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mes")
    public ResponseEntity<?> mesVehicules(@RequestHeader("Authorization") String authHeader) {
        UUID acteurId = jwtService.extraireActeurId(authHeader.substring(7));
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        return ResponseEntity.ok(vehiculeService.listerMesVehicules(acteurId, tenantId));
    }

    // Consomme en synchrone par service-cap (ServiceFltClient, meme porteur
    // Mobile, Plan d'execution S4.3) pour resoudre le proprietaire d'un
    // vehicule au moment de la declaration de capacite - ferme le bug S7.
    //
    // IDOR corrige (audit CDC du 19 aout, bloquant §3 "endpoint vehicule
    // public, sans filtre tenant") : cet endpoint etait permitAll() sans
    // aucune verification, exposant tout vehicule de tout tenant a
    // quiconque atteignait ce port. service-cap transmet desormais son
    // propre JWT (celui du transporteur qui declare sa capacite, meme
    // secret partage service-ida) au lieu d'un appel anonyme -- meme
    // exception "introuvable" pour "n'existe pas" et "pas votre tenant"
    // (meme principe que DossierController cote service-adm).
    @GetMapping("/{id}")
    public ResponseEntity<VehiculeDto.VehiculeResponse> consulter(@PathVariable UUID id,
                                                                    @RequestHeader("Authorization") String authHeader) {
        String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
        Vehicule vehicule = vehiculeRepository.findById(id)
                .filter(v -> tenantId.equals(v.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicule introuvable : " + id));
        return ResponseEntity.ok(VehiculeDto.VehiculeResponse.fromEntity(vehicule));
    }
}
