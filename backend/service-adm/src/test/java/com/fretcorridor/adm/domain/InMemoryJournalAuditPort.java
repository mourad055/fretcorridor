package com.fretcorridor.adm.domain;

import java.util.ArrayList;
import java.util.List;

public class InMemoryJournalAuditPort implements JournalAuditPort {

    private final List<EntreeJournalAudit> entrees = new ArrayList<>();

    @Override
    public void enregistrer(EntreeJournalAudit entree) {
        entrees.add(entree);
    }

    @Override
    public List<EntreeJournalAudit> lister(String tenantId) {
        return entrees.stream().filter(e -> tenantId == null || tenantId.equals(e.tenantId())).toList();
    }
}
