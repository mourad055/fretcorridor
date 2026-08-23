package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.Dossier;

import java.time.Instant;
import java.util.List;

public record DossierResponse(
        String id,
        String tenantId,
        String type,
        String priorite,
        String statut,
        String missionId,
        List<String> parties,
        List<String> preuvesReferences,
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
    public static DossierResponse from(Dossier dossier) {
        return new DossierResponse(
                dossier.id(),
                dossier.tenantId(),
                dossier.type().name(),
                dossier.priorite().name(),
                dossier.statut().name(),
                dossier.missionId(),
                dossier.parties(),
                dossier.preuvesReferences(),
                dossier.motif(),
                dossier.description(),
                dossier.ouvertLe(),
                dossier.delaiTraitement(),
                dossier.priseEnChargeParActeurId(),
                dossier.decision(),
                dossier.motifDecision(),
                dossier.decidePar(),
                dossier.decideLe(),
                dossier.grilleVersionAppliquee(),
                dossier.recoursDeDossierId()
        );
    }
}
