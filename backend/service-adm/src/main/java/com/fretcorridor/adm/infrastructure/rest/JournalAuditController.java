package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.AccesRefuseException;
import com.fretcorridor.adm.domain.JournalAuditService;
import com.fretcorridor.adm.infrastructure.rest.dto.EnregistrerAuditRequest;
import com.fretcorridor.adm.infrastructure.rest.dto.EntreeJournalAuditResponse;
import com.fretcorridor.adm.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FE-ADM-05 : journal d'audit consultable et exportable, en lecture seule, append-only. */
@RestController
@RequestMapping("/api/v1/journal-audit")
public class JournalAuditController {

    private final JournalAuditService journalAuditService;
    private final JwtService jwtService;

    public JournalAuditController(JournalAuditService journalAuditService, JwtService jwtService) {
        this.journalAuditService = journalAuditService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<EntreeJournalAuditResponse> enregistrer(@Valid @RequestBody EnregistrerAuditRequest request) {
        var entree = journalAuditService.enregistrer(request.tenantId(), request.acteurId(), request.action(),
                request.ressource());
        return ResponseEntity.status(201).body(EntreeJournalAuditResponse.from(entree));
    }

    // IDOR corrigé (audit CDC §7.2, "export cross-tenant si tenantId omis") :
    // sans ADMINISTRATION, la lecture est cantonnée au tenant du JWT --
    // tenantId omis ne dégénère plus en "tous les tenants".
    @GetMapping
    public List<EntreeJournalAuditResponse> lister(@RequestParam(required = false) String tenantId,
                                                     @RequestHeader("Authorization") String authHeader) {
        return journalAuditService.lister(tenantIdAutorise(tenantId, authHeader)).stream()
                .map(EntreeJournalAuditResponse::from).toList();
    }

    @GetMapping("/export")
    public ResponseEntity<String> exporter(@RequestParam(required = false) String tenantId,
                                            @RequestHeader("Authorization") String authHeader) {
        String csv = journalAuditService.exporterCsv(tenantIdAutorise(tenantId, authHeader));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"journal-audit.csv\"")
                .body(csv);
    }

    /**
     * Résout le tenantId à interroger : un tenant sans le rôle ADMINISTRATION
     * ne peut consulter/exporter que son propre journal (tenantId omis ->
     * son propre tenant, pas "tous"). ADMINISTRATION conserve tenantId omis
     * == tous les tenants, pour la consultation transverse légitime.
     */
    private String tenantIdAutorise(String tenantIdDemande, String authHeader) {
        String token = authHeader.substring(7);
        if (jwtService.extraireRoles(token).contains("ADMINISTRATION")) {
            return tenantIdDemande;
        }
        String tenantIdJwt = jwtService.extraireTenantId(token);
        if (tenantIdDemande != null && !tenantIdDemande.equals(tenantIdJwt)) {
            throw new AccesRefuseException();
        }
        return tenantIdJwt;
    }
}
