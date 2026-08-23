package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.TenantService;
import com.fretcorridor.adm.infrastructure.rest.dto.CreerTenantRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.TenantResponse;
import com.fretcorridor.adm.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** FE-ADM-04 : gestion des tenants (bureaux de fret, entités multi-tenant). */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final JwtService jwtService;

    public TenantController(TenantService tenantService, JwtService jwtService) {
        this.tenantService = tenantService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public List<TenantResponse> lister() {
        return tenantService.lister().stream().map(TenantResponse::from).toList();
    }

    // Bloquant audit CDC §Transverse ("tenantId lu du corps de requête") :
    // auteur vient désormais du JWT, jamais du corps (même principe que
    // DossierController).
    //
    // BUG CORRIGE (audit de suivi, 23 aout) : la creation de tenant exigeait
    // deja un JWT valide mais n'imposait aucun role - tout acteur
    // authentifie (chargeur, transporteur...) pouvait creer un tenant.
    // Meme pattern ADMINISTRATION que DossierController/JournalAuditController.
    @PostMapping
    public ResponseEntity<TenantResponse> creer(@Valid @RequestBody CreerTenantRequest request,
                                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtService.extraireRoles(token).contains("ADMINISTRATION")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul le role ADMINISTRATION peut creer un tenant");
        }
        String auteur = jwtService.extraireActeurId(token);
        var tenant = tenantService.creer(request.id(), request.nom(), request.pays(), auteur);
        return ResponseEntity.status(201).body(TenantResponse.from(tenant));
    }
}
