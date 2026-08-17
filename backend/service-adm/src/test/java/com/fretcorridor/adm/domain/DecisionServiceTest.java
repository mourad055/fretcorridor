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
    private final InMemoryDossierEventPort dossierEventPort = new InMemoryDossierEventPort();
    private final InMemoryConfigurationPort configurationPort = new InMemoryConfigurationPort();
    private final FileTravailService fileTravailService = new FileTravailService(dossierPort, journalAuditPort, dossierEventPort);
    private final DecisionService decisionService = new DecisionService(dossierPort, journalAuditPort, dossierEventPort, configurationPort);

    private void definirGrille(String tenantId) {
        configurationPort.sauvegarder(new ConfigurationVersionnee("g-1", DecisionService.CLE_GRILLE_DECISION,
                tenantId, "grille v1", "actor-admin-1", 1, Instant.now()));
    }

    @Test
    void trancher_un_dossier_le_clot_et_journalise_la_decision() {
        definirGrille("tenant-bgft-douala");
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of("acteur-transporteur-1"), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier tranche = decisionService.trancher(dossier.id(), "RESOLU_EN_FAVEUR_TRANSPORTEUR",
                "Preuve de livraison conforme", "actor-admin-1");

        assertThat(tranche.statut()).isEqualTo(StatutDossier.CLOS);
        assertThat(tranche.decision()).isEqualTo("RESOLU_EN_FAVEUR_TRANSPORTEUR");
        assertThat(tranche.decidePar()).isEqualTo("actor-admin-1");
        assertThat(tranche.grilleVersionAppliquee()).isEqualTo(1);
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_DECISION_RESOLU_EN_FAVEUR_TRANSPORTEUR"));
        assertThat(dossierEventPort.publies())
                .as("l'ouverture ET la clôture du litige doivent avoir été publiées")
                .hasSize(2);
        assertThat(dossierEventPort.publies().get(1).statut()).isEqualTo(StatutDossier.CLOS);
    }

    @Test
    void trancher_un_dossier_deja_clos_est_interdit() {
        definirGrille("tenant-bgft-douala");
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));
        decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1");

        assertThatThrownBy(() -> decisionService.trancher(dossier.id(), "AUTRE", "motif", "actor-admin-1"))
                .isInstanceOf(DossierDejaTrancheException.class);
    }

    /** RG-096 : un opérateur ne doit jamais avoir à trancher sans grille (E1, UC-ADM-01). */
    @Test
    void trancher_sans_grille_de_decision_definie_pour_le_tenant_est_refuse() {
        Dossier dossier = fileTravailService.ouvrir("tenant-sans-grille", TypeDossier.MODERATION, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1"))
                .isInstanceOf(GrilleDecisionAbsenteException.class);
    }

    @Test
    void deux_tenants_appliquent_des_versions_de_grille_independantes() {
        definirGrille("tenant-bgft-douala");
        definirGrille("tenant-bnft-ndjamena");
        configurationPort.sauvegarder(new ConfigurationVersionnee("g-2", DecisionService.CLE_GRILLE_DECISION,
                "tenant-bnft-ndjamena", "grille v2", "actor-admin-1", 2, Instant.now()));

        Dossier dossier = fileTravailService.ouvrir("tenant-bnft-ndjamena", TypeDossier.MODERATION, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier tranche = decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1");

        assertThat(tranche.grilleVersionAppliquee()).isEqualTo(2);
    }

    /** RG-098, garde en profondeur : même sans prise en charge préalable, trancher() refuse le même opérateur. */
    @Test
    void trancher_un_recours_par_le_meme_operateur_que_le_premier_decideur_est_refuse_meme_sans_prise_en_charge() {
        definirGrille("tenant-bgft-douala");
        FileTravailService fileTravailServiceLocal = new FileTravailService(dossierPort, journalAuditPort, dossierEventPort);
        Dossier original = fileTravailServiceLocal.ouvrir("tenant-bgft-douala", TypeDossier.MODERATION, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));
        Dossier tranche = decisionService.trancher(original.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1");
        Dossier recours = fileTravailServiceLocal.ouvrirRecours(tranche.id(), PrioriteDossier.HAUTE, Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> decisionService.trancher(recours.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1"))
                .isInstanceOf(RecoursMemeOperateurException.class);
    }
}
