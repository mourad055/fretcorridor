package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** FE-ADM-05 / ENF-SEC-02 : journal d'audit append-only. Aucune méthode de mise à jour ou de suppression. */
public class JournalAuditService {

    private final JournalAuditPort journalAuditPort;

    public JournalAuditService(JournalAuditPort journalAuditPort) {
        this.journalAuditPort = journalAuditPort;
    }

    public EntreeJournalAudit enregistrer(String tenantId, String acteurId, String action, String ressource) {
        EntreeJournalAudit entree = new EntreeJournalAudit(UUID.randomUUID().toString(), tenantId, acteurId, action,
                ressource, Instant.now());
        journalAuditPort.enregistrer(entree);
        return entree;
    }

    public List<EntreeJournalAudit> lister(String tenantId) {
        return journalAuditPort.lister(tenantId);
    }

    public String exporterCsv(String tenantId) {
        StringBuilder csv = new StringBuilder("id,tenantId,acteurId,action,ressource,horodatage\n");
        for (EntreeJournalAudit entree : journalAuditPort.lister(tenantId)) {
            csv.append(entree.id()).append(',')
                    .append(entree.tenantId() == null ? "" : entree.tenantId()).append(',')
                    .append(entree.acteurId()).append(',')
                    .append(entree.action()).append(',')
                    .append(entree.ressource()).append(',')
                    .append(entree.horodatage())
                    .append('\n');
        }
        return csv.toString();
    }
}
