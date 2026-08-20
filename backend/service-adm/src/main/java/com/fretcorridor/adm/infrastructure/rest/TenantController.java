package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.TenantService;
import com.fretcorridor.adm.infrastructure.rest.dto.CreerTenantRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.TenantResponse;
import com.fretcorridor.adm.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping
    public ResponseEntity<TenantResponse> creer(@Valid @RequestBody CreerTenantRequest request,
                                                  @RequestHeader("Authorization") String authHeader) {
        String auteur = jwtService.extraireActeurId(authHeader.substring(7));
        var tenant = tenantService.creer(request.id(), request.nom(), request.pays(), auteur);
        return ResponseEntity.status(201).body(TenantResponse.from(tenant));
    }
}
