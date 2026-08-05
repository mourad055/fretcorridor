package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JournalAuditServiceTest {

    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final JournalAuditService service = new JournalAuditService(journalAuditPort);

    @Test
    void exporter_le_journal_en_csv_inclut_une_ligne_par_entree() {
        journalAuditPort.enregistrer(new EntreeJournalAudit("entree-1", "tenant-bgft-douala", "actor-admin-1",
                "DOSSIER_OUVERT", "dossier:dossier-1", Instant.parse("2026-08-05T10:00:00Z")));

        String csv = service.exporterCsv("tenant-bgft-douala");

        assertThat(csv).startsWith("id,tenantId,acteurId,action,ressource,horodatage\n");
        assertThat(csv).contains("entree-1,tenant-bgft-douala,actor-admin-1,DOSSIER_OUVERT,dossier:dossier-1");
    }
}
