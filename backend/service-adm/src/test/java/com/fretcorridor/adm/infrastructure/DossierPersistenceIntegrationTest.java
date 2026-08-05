package com.fretcorridor.adm.infrastructure;

import com.fretcorridor.adm.domain.DecisionService;
import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.FileTravailService;
import com.fretcorridor.adm.domain.PrioriteDossier;
import com.fretcorridor.adm.domain.TypeDossier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Preuve que la file de travail et les décisions persistent réellement en Postgres. */
@SpringBootTest
@Testcontainers
class DossierPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private FileTravailService fileTravailService;

    @Autowired
    private DecisionService decisionService;

    @Test
    void un_dossier_ouvert_avec_ses_parties_et_preuves_est_relu_depuis_postgres() {
        String tenantId = "tenant-test-" + System.nanoTime();

        Dossier ouvert = fileTravailService.ouvrir(tenantId, TypeDossier.LITIGE, PrioriteDossier.HAUTE, "mission-a",
                List.of("acteur-transporteur-1", "acteur-bureau-1"), List.of("preuve-photo-1"),
                Instant.now().plus(2, ChronoUnit.DAYS));

        List<Dossier> file = fileTravailService.lister(tenantId);

        assertThat(file).hasSize(1);
        assertThat(file.get(0).id()).isEqualTo(ouvert.id());
        assertThat(file.get(0).parties()).containsExactly("acteur-transporteur-1", "acteur-bureau-1");
        assertThat(file.get(0).preuvesReferences()).containsExactly("preuve-photo-1");
    }

    @Test
    void une_decision_persistee_clot_le_dossier_de_facon_durable() {
        String tenantId = "tenant-test-" + System.nanoTime();
        Dossier dossier = fileTravailService.ouvrir(tenantId, TypeDossier.INCIDENT, PrioriteDossier.NORMALE, null,
                List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        decisionService.trancher(dossier.id(), "CLOS_SANS_SUITE", "Incident résolu", "actor-admin-1");

        List<Dossier> file = fileTravailService.lister(tenantId);
        assertThat(file.get(0).decision()).isEqualTo("CLOS_SANS_SUITE");
        assertThat(file.get(0).decidePar()).isEqualTo("actor-admin-1");
    }
}
