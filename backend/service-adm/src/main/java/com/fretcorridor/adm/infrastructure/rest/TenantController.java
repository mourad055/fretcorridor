package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.TenantService;
import com.fretcorridor.adm.infrastructure.rest.dto.CreerTenantRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FE-ADM-04 : gestion des tenants (bureaux de fret, entités multi-tenant). */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponse> lister() {
        return tenantService.lister().stream().map(TenantResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TenantResponse> creer(@Valid @RequestBody CreerTenantRequest request) {
        var tenant = tenantService.creer(request.id(), request.nom(), request.pays(), request.auteur());
        return ResponseEntity.status(201).body(TenantResponse.from(tenant));
    }
}
