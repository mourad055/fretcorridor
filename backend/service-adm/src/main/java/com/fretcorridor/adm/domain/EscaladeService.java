package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Escalade automatique sur dépassement de délai (Sprint 10, DoD PRD §9).
 * TODO(adm): remplacer l'appel explicite par un job planifié une fois
 * l'ordonnanceur du back-office livré (Phase 2) ; en Phase 1 ce contrôle est
 * déclenché à la demande (endpoint dédié), ce qui reste suffisant pour la
 * preuve du parcours E2E.
 */
public class EscaladeService {

    private final DossierPort dossierPort;
    private final JournalAuditPort journalAuditPort;

    public EscaladeService(DossierPort dossierPort, JournalAuditPort journalAuditPort) {
        this.dossierPort = dossierPort;
        this.journalAuditPort = journalAuditPort;
    }

    public List<Dossier> detecterEtEscalader(Instant maintenant) {
        return dossierPort.listerTous().stream()
                .filter(dossier -> dossier.delaiDepasse(maintenant))
                .map(dossier -> {
                    Dossier escalade = dossier.escalader();
                    dossierPort.sauvegarder(escalade);
                    journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(),
                            dossier.tenantId(), "system", "DOSSIER_ESCALADE_AUTOMATIQUE_DELAI_DEPASSE",
                            "dossier:" + dossier.id(), maintenant));
                    return escalade;
                })
                .toList();
    }
}
