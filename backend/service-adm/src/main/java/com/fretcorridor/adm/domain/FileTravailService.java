package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** FE-ADM-01 : file de travail priorisée (modération, incidents, litiges). */
public class FileTravailService {

    private static final Comparator<Dossier> PAR_PRIORITE_PUIS_DELAI =
            Comparator.comparing((Dossier d) -> d.priorite().ordinal()).reversed()
                    .thenComparing(Dossier::delaiTraitement);

    private final DossierPort dossierPort;
    private final JournalAuditPort journalAuditPort;
    private final DossierEventPort dossierEventPort;

    public FileTravailService(DossierPort dossierPort, JournalAuditPort journalAuditPort, DossierEventPort dossierEventPort) {
        this.dossierPort = dossierPort;
        this.journalAuditPort = journalAuditPort;
        this.dossierEventPort = dossierEventPort;
    }

    public Dossier ouvrir(String tenantId, TypeDossier type, PrioriteDossier priorite, String missionId,
                           List<String> parties, List<String> preuvesReferences, String motif, String description,
                           Instant delaiTraitement) {
        return creerEtPublier(tenantId, type, priorite, missionId, parties, preuvesReferences, motif, description,
                delaiTraitement, null, "DOSSIER_OUVERT");
    }

    /**
     * UC-ADM-01 A2/RG-098 : une partie conteste une décision rendue — ouvre un
     * second Dossier lié à l'original (même mission/parties/preuves), pas une
     * réouverture du premier (invariant "jamais rouvrir un dossier déjà
     * tranché" préservé). La garde "opérateur différent" s'applique à la
     * prise en charge ({@link #prendreEnCharge}) et à la décision
     * ({@link DecisionService#trancher}) de ce second Dossier.
     */
    public Dossier ouvrirRecours(String dossierOriginalId, PrioriteDossier priorite, Instant delaiTraitement) {
        Dossier original = dossierPort.parId(dossierOriginalId)
                .orElseThrow(() -> new DossierIntrouvableException(dossierOriginalId));
        if (original.statut() != StatutDossier.CLOS) {
            throw new DossierNonTrancheException(dossierOriginalId);
        }
        return creerEtPublier(original.tenantId(), original.type(), priorite, original.missionId(),
                original.parties(), original.preuvesReferences(), original.motif(), original.description(),
                delaiTraitement, dossierOriginalId, "DOSSIER_RECOURS_OUVERT");
    }

    private Dossier creerEtPublier(String tenantId, TypeDossier type, PrioriteDossier priorite, String missionId,
                                    List<String> parties, List<String> preuvesReferences, String motif,
                                    String description, Instant delaiTraitement, String recoursDeDossierId,
                                    String actionJournal) {
        Dossier dossier = new Dossier(UUID.randomUUID().toString(), tenantId, type, priorite, StatutDossier.OUVERT,
                missionId, parties, preuvesReferences, motif, description, Instant.now(), delaiTraitement, null,
                null, null, null, null, null, recoursDeDossierId);
        dossierPort.sauvegarder(dossier);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), tenantId, "system",
                actionJournal, "dossier:" + dossier.id(), Instant.now()));
        if (type == TypeDossier.LITIGE && missionId != null) {
            dossierEventPort.publier(dossier);
        }
        return dossier;
    }

    /**
     * EF-PAY-09, ENF-FIN-03 : ouvre un incident sur écart de réconciliation —
     * pas de doublon tant qu'un incident reste ouvert pour la mission (le
     * balayage quotidien de service-pay republie l'événement chaque jour
     * tant que l'écart n'est pas résolu, ça ne doit pas spammer la file).
     */
    public Optional<Dossier> ouvrirIncidentReconciliation(String tenantId, String missionId, String description,
                                                            Instant delaiTraitement) {
        boolean dejaOuvert = dossierPort.lister(tenantId).stream()
                .anyMatch(d -> d.type() == TypeDossier.INCIDENT && missionId.equals(d.missionId())
                        && d.statut() != StatutDossier.CLOS);
        if (dejaOuvert) {
            return Optional.empty();
        }
        return Optional.of(creerEtPublier(tenantId, TypeDossier.INCIDENT, PrioriteDossier.HAUTE, missionId,
                List.of(), List.of(description), null, description, delaiTraitement, null,
                "DOSSIER_INCIDENT_RECONCILIATION_OUVERT"));
    }

    public List<Dossier> lister(String tenantId) {
        return dossierPort.lister(tenantId).stream().sorted(PAR_PRIORITE_PUIS_DELAI).toList();
    }

    // IDOR corrigé (audit CDC du 19 août, §7.2 : "Dossier de litige lisible
    // par ID sans vérification de tenant") - même exception pour "introuvable"
    // et "pas le sien", même principe que capaciteAppartenantA
    // (service-cap) / missionAppartenantA (service-exe) / notificationAppartenantA
    // (service-not).
    // ENF-SEC-02 (audit UX 2026-08-23, docs/AUDIT_ROADMAP_Backoffice_Web_
    // 2026-08-23.md §1.8) : la simple consultation d'un dossier (contrairement
    // à prendreEnCharge/trancher) n'était jusqu'ici jamais journalisée --
    // écart documenté depuis l'audit CDC du 19 août, comblé ici.
    public Dossier consulter(String dossierId, String tenantId, String acteurId) {
        Dossier dossier = dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
        if (!dossier.tenantId().equals(tenantId)) {
            throw new DossierIntrouvableException(dossierId);
        }
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), tenantId, acteurId,
                "DOSSIER_CONSULTE", "dossier:" + dossierId, Instant.now()));
        return dossier;
    }

    public Dossier prendreEnCharge(String dossierId, String acteurId) {
        Dossier dossier = dossierPort.parId(dossierId).orElseThrow(() -> new DossierIntrouvableException(dossierId));
        verifierOperateurDifferentDuPremierDecideur(dossier, acteurId);
        Dossier misAJour = dossier.priseEnCharge(acteurId);
        dossierPort.sauvegarder(misAJour);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(), dossier.tenantId(),
                acteurId, "DOSSIER_PRIS_EN_CHARGE", "dossier:" + dossierId, Instant.now()));
        return misAJour;
    }

    /** RG-098 : l'opérateur ayant décidé en premier ressort n'instruit pas le recours. */
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
