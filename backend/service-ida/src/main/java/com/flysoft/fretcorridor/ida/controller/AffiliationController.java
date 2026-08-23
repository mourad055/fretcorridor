package com.flysoft.fretcorridor.ida.controller;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.security.JwtService;
import com.flysoft.fretcorridor.ida.service.AffiliationService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** S18 (Sprint 18, "Second tenant institutionnel") — cf. javadoc AffiliationService. */
@RestController
@RequestMapping("/api/ida/affiliations")
@RequiredArgsConstructor
public class AffiliationController {

    private final AffiliationService affiliationService;
    private final JwtService jwtService;

    @Data
    public static class InviterRequest {
        @NotBlank private String telephone;
    }

    @Data
    public static class SelectionnerRequest {
        @NotBlank private String tenantId;
    }

    // Reserve au role BUREAU du tenant invitant - tenantId pris du JWT de
    // l'appelant, jamais du corps de requete (meme principe que
    // TenantController/DossierController, service-adm).
    @PostMapping
    public ResponseEntity<?> inviter(@jakarta.validation.Valid @RequestBody InviterRequest request,
                                      @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtService.extraireRoles(token).contains("BUREAU")) {
            return ResponseEntity.status(403).body("ROLE_BUREAU_REQUIS");
        }
        try {
            affiliationService.inviter(jwtService.extraireTenantId(token), request.getTelephone());
            return ResponseEntity.status(201).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mes-tenants")
    public ResponseEntity<List<AuthDto.TenantDisponible>> mesTenants(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(affiliationService.mesTenants(jwtService.extraireActeurId(authHeader.substring(7))));
    }

    @PostMapping("/selection")
    public ResponseEntity<?> selectionner(@jakarta.validation.Valid @RequestBody SelectionnerRequest request,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            var acteurId = jwtService.extraireActeurId(authHeader.substring(7));
            return ResponseEntity.ok(affiliationService.selectionner(acteurId, request.getTenantId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
