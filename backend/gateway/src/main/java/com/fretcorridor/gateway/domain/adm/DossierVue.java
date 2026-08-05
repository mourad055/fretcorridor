package com.fretcorridor.gateway.domain.adm;

import java.time.Instant;
import java.util.List;

/** Vue en lecture seule d'un dossier de service-adm (Sprint 10). */
public record DossierVue(
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
}
