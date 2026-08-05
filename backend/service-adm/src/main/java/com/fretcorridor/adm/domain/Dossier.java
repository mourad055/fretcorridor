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
        Instant ouvertLe,
        Instant delaiTraitement,
        String priseEnChargeParActeurId,
        String decision,
        String motifDecision,
        String decidePar,
        Instant decideLe
) {
    public Dossier priseEnCharge(String acteurId) {
        if (statut == StatutDossier.CLOS) {
            throw new DossierDejaTrancheException(id);
        }
        return new Dossier(id, tenantId, type, priorite, StatutDossier.EN_COURS, missionId, parties,
                preuvesReferences, ouvertLe, delaiTraitement, acteurId, decision, motifDecision, decidePar, decideLe);
    }

    public Dossier trancher(String decisionPrise, String motif, String acteurId, Instant maintenant) {
        if (statut == StatutDossier.CLOS) {
            throw new DossierDejaTrancheException(id);
        }
        return new Dossier(id, tenantId, type, priorite, StatutDossier.CLOS, missionId, parties,
                preuvesReferences, ouvertLe, delaiTraitement, priseEnChargeParActeurId, decisionPrise, motif,
                acteurId, maintenant);
    }

    public Dossier escalader() {
        return new Dossier(id, tenantId, type, PrioriteDossier.HAUTE, StatutDossier.ESCALADE, missionId, parties,
                preuvesReferences, ouvertLe, delaiTraitement, priseEnChargeParActeurId, decision, motifDecision,
                decidePar, decideLe);
    }

    public boolean delaiDepasse(Instant maintenant) {
        return (statut == StatutDossier.OUVERT || statut == StatutDossier.EN_COURS)
                && delaiTraitement.isBefore(maintenant);
    }
}
