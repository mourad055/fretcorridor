package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** DoD PRD §9 S10 : escalade automatique testée sur dépassement de délai. */
class EscaladeServiceTest {

    private final InMemoryDossierPort dossierPort = new InMemoryDossierPort();
    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final InMemoryDossierEventPort dossierEventPort = new InMemoryDossierEventPort();
    private final FileTravailService fileTravailService = new FileTravailService(dossierPort, journalAuditPort, dossierEventPort);
    private final EscaladeService escaladeService = new EscaladeService(dossierPort, journalAuditPort);

    @Test
    void un_dossier_dont_le_delai_est_depasse_est_escalade_en_haute_priorite() {
        Instant maintenant = Instant.now();
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.BASSE,
                null, List.of(), List.of(), null, null, maintenant.minus(1, ChronoUnit.HOURS));

        List<Dossier> escalades = escaladeService.detecterEtEscalader(maintenant);

        assertThat(escalades).extracting(Dossier::id).containsExactly(dossier.id());
        assertThat(escalades.get(0).statut()).isEqualTo(StatutDossier.ESCALADE);
        assertThat(escalades.get(0).priorite()).isEqualTo(PrioriteDossier.HAUTE);
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_ESCALADE_AUTOMATIQUE_DELAI_DEPASSE"));
    }

    @Test
    void un_dossier_dont_le_delai_n_est_pas_depasse_n_est_pas_escalade() {
        Instant maintenant = Instant.now();
        fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.BASSE, null, List.of(),
                List.of(), null, null, maintenant.plus(1, ChronoUnit.DAYS));

        assertThat(escaladeService.detecterEtEscalader(maintenant)).isEmpty();
    }

    @Test
    void un_dossier_deja_clos_n_est_jamais_escalade_meme_si_le_delai_est_depasse() {
        Instant maintenant = Instant.now();
        Dossier dossier = fileTravailService.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.BASSE,
                null, List.of(), List.of(), null, null, maintenant.minus(1, ChronoUnit.HOURS));
        InMemoryConfigurationPort configurationPort = new InMemoryConfigurationPort();
        configurationPort.sauvegarder(new ConfigurationVersionnee("g-1", DecisionService.CLE_GRILLE_DECISION,
                "tenant-bgft-douala", "grille v1", "actor-admin-1", 1, Instant.now()));
        DecisionService decisionService = new DecisionService(dossierPort, journalAuditPort, dossierEventPort, configurationPort);
        decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "motif", "actor-admin-1");

        assertThat(escaladeService.detecterEtEscalader(maintenant)).isEmpty();
    }
}
