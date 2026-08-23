package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;

/**
 * File de travail priorisée (FE-ADM-01) et dossier consolidé (FE-ADM-02, en
 * partie — la chronologie de mission et les écritures de paiement sont
 * agrégées côté gateway, qui seul peut consommer service-exe/service-pay de
 * façon synchrone, cf. PRD §4.2).
 */
public record Dossier(
        String id,
        String tenantId,
        TypeDossier type,
        PrioriteDossier priorite,
        StatutDossier statut,
        String missionId,
        List<String> parties,
        List<String> preuvesReferences,
        // Motif/description (audit de suivi, 23 aout) : contenu libre saisi
        // par l'auteur du signalement (ex. chargeur, S19) - jusqu'ici absent
        // du contrat, qui ne portait que des references structurees
        // (parties/preuves), pensees pour un dossier ouvert cote ADM plutot
        // que pour la plainte initiale d'un utilisateur. Nullables : les
        // autres types de dossier (MODERATION/INCIDENT) n'en fournissent
        // pas forcement.
        String motif,
        String description,
        Instant ouvertLe,
        Instant delaiTraitement,
        String priseEnChargeParActeurId,
        String decision,
        String motifDecision,
        String decidePar,
        Instant decideLe,
        Integer grilleVersionAppliquee,
        String recoursDeDossierId
) {
    public Dossier priseEnCharge(String acteurId) {
        if (statut == StatutDossier.CLOS) {
            throw new DossierDejaTrancheException(id);
        }
        return new Dossier(id, tenantId, type, priorite, StatutDossier.EN_COURS, missionId, parties,
                preuvesReferences, motif, description, ouvertLe, delaiTraitement, acteurId, decision, motifDecision,
                decidePar, decideLe, grilleVersionAppliquee, recoursDeDossierId);
    }

    /** RG-096 : {@code grilleVersion} est la version de la grille de décision appliquée, enregistrée avec la décision. */
    public Dossier trancher(String decisionPrise, String motifDecision, String acteurId, Instant maintenant, int grilleVersion) {
        if (statut == StatutDossier.CLOS) {
            throw new DossierDejaTrancheException(id);
        }
        return new Dossier(id, tenantId, type, priorite, StatutDossier.CLOS, missionId, parties,
                preuvesReferences, motif, description, ouvertLe, delaiTraitement, priseEnChargeParActeurId,
                decisionPrise, motifDecision, acteurId, maintenant, grilleVersion, recoursDeDossierId);
    }

    public Dossier escalader() {
        return new Dossier(id, tenantId, type, PrioriteDossier.HAUTE, StatutDossier.ESCALADE, missionId, parties,
                preuvesReferences, motif, description, ouvertLe, delaiTraitement, priseEnChargeParActeurId, decision,
                motifDecision, decidePar, decideLe, grilleVersionAppliquee, recoursDeDossierId);
    }

    public boolean delaiDepasse(Instant maintenant) {
        return (statut == StatutDossier.OUVERT || statut == StatutDossier.EN_COURS)
                && delaiTraitement.isBefore(maintenant);
    }
}
