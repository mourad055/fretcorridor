package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.EntreeJournalAuditResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** FE-ADM-05 : journal d'audit consultable et exportable, en lecture seule, append-only. */
@RestController
@RequestMapping("/api/v1/admin/journal-audit")
public class JournalAuditController {

    private final AdmPort admPort;

    public JournalAuditController(AdmPort admPort) {
        this.admPort = admPort;
    }

    @GetMapping
    public Flux<EntreeJournalAuditResponse> lister(@RequestParam(required = false) String tenantId) {
        return admPort.journalAudit(tenantId).map(EntreeJournalAuditResponse::from);
    }

    @GetMapping("/export")
    public Mono<ResponseEntity<String>> exporter(@RequestParam(required = false) String tenantId) {
        return admPort.exporterJournalAudit(tenantId)
                .map(csv -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"journal-audit.csv\"")
                        .body(csv));
    }
}
