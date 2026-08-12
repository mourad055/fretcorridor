package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * FE-ADM-02 : décision de clôture d'un dossier, versionnée par l'historique
 * du journal d'audit — jamais de suppression, seulement de nouvelles entrées.
 */
public class DecisionService {

    private final DossierPort dossierPort;
    private final JournalAuditPort journalAuditPort;
    private final DossierEventPort dossierEventPort;

    public DecisionService(DossierPort dossierPort, JournalAuditPort journalAuditPort, DossierEventPort dossierEventPort) {
        this.dossierPort = dossierPort;
        this.journalAuditPort = journalAuditPort;
        this.dossierEventPort = dossierEventPort;
    }

    public Dossier trancher(String dossierId, String decision, String motif, String acteurId) {
        Dossier dossier = dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
        Dossier tranche = dossier.trancher(decision, motif, acteurId, Instant.now());
        dossierPort.sauvegarder(tranche);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), dossier.tenantId(),
                acteurId, "DOSSIER_DECISION_" + decision, "dossier:" + dossierId, Instant.now()));
        if (tranche.type() == TypeDossier.LITIGE && tranche.missionId() != null) {
            dossierEventPort.publier(tranche);
        }
        return tranche;
    }
}
