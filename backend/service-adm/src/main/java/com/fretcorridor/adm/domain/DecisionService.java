package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * FE-ADM-02 : décision de clôture d'un dossier, versionnée par l'historique
 * du journal d'audit — jamais de suppression, seulement de nouvelles entrées.
 * EF-ADM-03/RG-096 : toute décision s'appuie sur la grille de décision
 * versionnée courante du tenant (réutilise {@link ConfigurationVersionnee},
 * clé {@value #CLE_GRILLE_DECISION}, périmètre = tenantId) — pas de grille
 * définie pour ce tenant, pas de décision possible.
 */
public class DecisionService {

    public static final String CLE_GRILLE_DECISION = "grille-decision";

    private final DossierPort dossierPort;
    private final JournalAuditPort journalAuditPort;
    private final DossierEventPort dossierEventPort;
    private final ConfigurationPort configurationPort;

    public DecisionService(DossierPort dossierPort, JournalAuditPort journalAuditPort,
                            DossierEventPort dossierEventPort, ConfigurationPort configurationPort) {
        this.dossierPort = dossierPort;
        this.journalAuditPort = journalAuditPort;
        this.dossierEventPort = dossierEventPort;
        this.configurationPort = configurationPort;
    }

    public Dossier trancher(String dossierId, String decision, String motif, String acteurId) {
        Dossier dossier = dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
        verifierOperateurDifferentDuPremierDecideur(dossier, acteurId);
        ConfigurationVersionnee grille = configurationPort.versionCourante(CLE_GRILLE_DECISION, dossier.tenantId())
                .orElseThrow(() -> new GrilleDecisionAbsenteException(dossier.tenantId()));
        Dossier tranche = dossier.trancher(decision, motif, acteurId, Instant.now(), grille.version());
        dossierPort.sauvegarder(tranche);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), dossier.tenantId(),
                acteurId, "DOSSIER_DECISION_" + decision, "dossier:" + dossierId, Instant.now()));
        if (tranche.type() == TypeDossier.LITIGE && tranche.missionId() != null) {
            dossierEventPort.publier(tranche);
        }
        return tranche;
    }

    /** RG-098 : l'opérateur ayant décidé en premier ressort n'instruit pas le recours (garde redondante avec FileTravailService.prendreEnCharge, en profondeur). */
    private void verifierOperateurDifferentDuPremierDecideur(Dossier dossier, String acteurId) {
        if (dossier.recoursDeDossierId() == null) {
            return;
        }
        Dossier original = dossierPort.parId(dossier.recoursDeDossierId())
                .orElseThrow(() -> new DossierIntrouvableException(dossier.recoursDeDossierId()));
        if (acteurId.equals(original.decidePar())) {
            throw new RecoursMemeOperateurException(dossier.id(), acteurId);
        }
    }
}
