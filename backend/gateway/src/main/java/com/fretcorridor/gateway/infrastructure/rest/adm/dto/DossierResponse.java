package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import com.fretcorridor.gateway.domain.adm.DossierVue;

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
        Instant ouvertLe,
        Instant delaiTraitement,
        String priseEnChargeParActeurId,
        String decision,
        String motifDecision,
        String decidePar,
        Instant decideLe
) {
    public static DossierResponse from(DossierVue dossier) {
        return new DossierResponse(dossier.id(), dossier.tenantId(), dossier.type(), dossier.priorite(),
                dossier.statut(), dossier.missionId(), dossier.parties(), dossier.preuvesReferences(),
                dossier.ouvertLe(), dossier.delaiTraitement(), dossier.priseEnChargeParActeurId(),
                dossier.decision(), dossier.motifDecision(), dossier.decidePar(), dossier.decideLe());
    }
}
