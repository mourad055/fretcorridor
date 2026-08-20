package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EF-ADM-05 (Phase 3) : escalade automatique sur dépassement de délai
 * plafond (CDC UC-ADM-01, RG-099). Domaine pur, sans dépendance à Spring —
 * testable sans mock de framework. Déclenché périodiquement par
 * {@code EscaladeScheduler} (infra) ; l'endpoint REST dédié
 * (`POST /api/v1/dossiers/escalade`, Phase 1) reste disponible comme
 * déclenchement manuel, les deux appellent la même logique.
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
