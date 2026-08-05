package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** FE-ADM-01 : file de travail priorisée (modération, incidents, litiges). */
public class FileTravailService {

    private static final Comparator<Dossier> PAR_PRIORITE_PUIS_DELAI =
            Comparator.comparing((Dossier d) -> d.priorite().ordinal()).reversed()
                    .thenComparing(Dossier::delaiTraitement);

    private final DossierPort dossierPort;
    private final JournalAuditPort journalAuditPort;

    public FileTravailService(DossierPort dossierPort, JournalAuditPort journalAuditPort) {
        this.dossierPort = dossierPort;
        this.journalAuditPort = journalAuditPort;
    }

    public Dossier ouvrir(String tenantId, TypeDossier type, PrioriteDossier priorite, String missionId,
                           List<String> parties, List<String> preuvesReferences, Instant delaiTraitement) {
        Dossier dossier = new Dossier(UUID.randomUUID().toString(), tenantId, type, priorite, StatutDossier.OUVERT,
                missionId, parties, preuvesReferences, Instant.now(), delaiTraitement, null, null, null, null, null);
        dossierPort.sauvegarder(dossier);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), tenantId, "system",
                "DOSSIER_OUVERT", "dossier:" + dossier.id(), Instant.now()));
        return dossier;
    }

    public List<Dossier> lister(String tenantId) {
        return dossierPort.lister(tenantId).stream().sorted(PAR_PRIORITE_PUIS_DELAI).toList();
    }

    public Dossier consulter(String dossierId) {
        return dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
    }

    public Dossier prendreEnCharge(String dossierId, String acteurId) {
        Dossier dossier = dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
        Dossier misAJour = dossier.priseEnCharge(acteurId);
        dossierPort.sauvegarder(misAJour);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), dossier.tenantId(),
                acteurId, "DOSSIER_PRIS_EN_CHARGE", "dossier:" + dossierId, Instant.now()));
        return misAJour;
    }
}
