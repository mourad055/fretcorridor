package com.fretcorridor.adm.domain;

import java.util.List;

/** Append-only par construction : aucune méthode de suppression ou de mise à jour n'est exposée. */
public interface JournalAuditPort {
    void enregistrer(EntreeJournalAudit entree);

    List<EntreeJournalAudit> lister(String tenantId);
}
