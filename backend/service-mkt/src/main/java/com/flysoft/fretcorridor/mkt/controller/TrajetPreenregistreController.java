package com.flysoft.fretcorridor.mkt.controller;

import com.flysoft.fretcorridor.mkt.dto.TrajetPreenregistreDto;
import com.flysoft.fretcorridor.mkt.security.JwtService;
import com.flysoft.fretcorridor.mkt.service.TrajetPreenregistreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

// Trajets preenregistres (plan §5) : raccourci de saisie, pre-remplit le
// formulaire de publication -- jamais publie seul, voir javadoc entite.
@RestController
@RequestMapping("/api/trajets-preenregistres")
@RequiredArgsConstructor
public class TrajetPreenregistreController {

    private final TrajetPreenregistreService trajetService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<?> enregistrer(
            @Valid @RequestBody TrajetPreenregistreDto.EnregistrerRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID acteurId = jwtService.extraireActeurId(token);
            String tenantId = jwtService.extraireTenantId(token);
            var trajet = trajetService.enregistrer(request, acteurId, tenantId);
            return ResponseEntity.status(201).body(TrajetPreenregistreDto.TrajetResponse.fromEntity(trajet));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mes")
    public ResponseEntity<?> getMesTrajets(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID acteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        var reponse = trajetService.listerMesTrajets(acteurId, tenantId).stream()
                .map(TrajetPreenregistreDto.TrajetResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID acteurId = jwtService.extraireActeurId(token);
            String tenantId = jwtService.extraireTenantId(token);
            trajetService.supprimer(id, acteurId, tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if ("TRAJET_INTROUVABLE".equals(e.getMessage())) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
