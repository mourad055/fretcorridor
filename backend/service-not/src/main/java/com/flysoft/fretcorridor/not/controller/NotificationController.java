package com.flysoft.fretcorridor.not.controller;

import com.flysoft.fretcorridor.not.dto.NotificationDto;
import com.flysoft.fretcorridor.not.security.JwtService;
import com.flysoft.fretcorridor.not.service.NotificationIntrouvableException;
import com.flysoft.fretcorridor.not.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<?> getMesNotifications(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID acteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        return ResponseEntity.ok(notificationService.getMesNotifications(acteurId, tenantId));
    }

    @GetMapping("/non-lues/nombre")
    public ResponseEntity<?> getNombreNonLues(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID acteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        return ResponseEntity.ok(Map.of("nombre", notificationService.getNombreNonLues(acteurId, tenantId)));
    }

    @PatchMapping("/{id}/lue")
    public ResponseEntity<?> marquerCommeLue(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        try {
            notificationService.marquerCommeLue(id, jwtService.extraireActeurId(authHeader.substring(7)));
            return ResponseEntity.noContent().build();
        } catch (NotificationIntrouvableException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // S12 — acceptation/refus d'une proposition de retour à vide.
    @PatchMapping("/{id}/repondre")
    public ResponseEntity<?> repondre(@PathVariable UUID id, @RequestBody NotificationDto.RepondreRequest requete,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            notificationService.repondre(id, jwtService.extraireActeurId(authHeader.substring(7)), requete.isAccepte());
            return ResponseEntity.noContent().build();
        } catch (NotificationIntrouvableException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Préparé pour le vrai push — enregistrer maintenant, exploiter plus tard
    @PostMapping("/fcm-token")
    public ResponseEntity<?> enregistrerToken(
            @Valid @RequestBody NotificationDto.EnregistrerTokenRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID acteurId = jwtService.extraireActeurId(token);
        String tenantId = jwtService.extraireTenantId(token);
        notificationService.enregistrerToken(acteurId, request.getToken(), tenantId);
        return ResponseEntity.noContent().build();
    }
}
