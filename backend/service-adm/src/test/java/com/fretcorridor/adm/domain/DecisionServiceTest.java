package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionServiceTest {

    private final InMemoryDossierPort dossierPort = new InMemoryDossierPort();
    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final FileTravailService fileTravailService = new FileTravailService(dossierPort, journalAuditPort);
    private final DecisionService decisionService = new DecisionService(dossierPort, journalAuditPort);

    @Test
    void trancher_un_dossier_le_clot_et_journalise_la_decision() {
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of("acteur-transporteur-1"), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier tranche = decisionService.trancher(dossier.id(), "RESOLU_EN_FAVEUR_TRANSPORTEUR",
                "Preuve de livraison conforme", "actor-admin-1");

        assertThat(tranche.statut()).isEqualTo(StatutDossier.CLOS);
        assertThat(tranche.decision()).isEqualTo("RESOLU_EN_FAVEUR_TRANSPORTEUR");
        assertThat(tranche.decidePar()).isEqualTo("actor-admin-1");
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_DECISION_RESOLU_EN_FAVEUR_TRANSPORTEUR"));
    }

    @Test
    void trancher_un_dossier_deja_clos_est_interdit() {
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));
        decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1");

        assertThatThrownBy(() -> decisionService.trancher(dossier.id(), "AUTRE", "motif", "actor-admin-1"))
                .isInstanceOf(DossierDejaTrancheException.class);
    }
}
