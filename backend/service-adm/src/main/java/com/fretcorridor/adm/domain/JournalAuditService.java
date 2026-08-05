package com.fretcorridor.adm.domain;

import java.util.List;

/** FE-ADM-05 : journal d'audit consultable et exportable, en lecture seule, append-only. */
public class JournalAuditService {

    private final JournalAuditPort journalAuditPort;

    public JournalAuditService(JournalAuditPort journalAuditPort) {
        this.journalAuditPort = journalAuditPort;
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
