package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.JournalAuditService;
import com.fretcorridor.adm.infrastructure.rest.dto.EntreeJournalAuditResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** FE-ADM-05 : journal d'audit consultable et exportable, en lecture seule, append-only. */
@RestController
@RequestMapping("/api/v1/journal-audit")
public class JournalAuditController {

    private final JournalAuditService journalAuditService;

    public JournalAuditController(JournalAuditService journalAuditService) {
        this.journalAuditService = journalAuditService;
    }

    @GetMapping
    public List<EntreeJournalAuditResponse> lister(@RequestParam(required = false) String tenantId) {
        return journalAuditService.lister(tenantId).stream().map(EntreeJournalAuditResponse::from).toList();
    }

    @GetMapping("/export")
    public ResponseEntity<String> exporter(@RequestParam(required = false) String tenantId) {
        String csv = journalAuditService.exporterCsv(tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"journal-audit.csv\"")
                .body(csv);
    }
}
