package com.fretcorridor.bur.infrastructure;

import com.fretcorridor.bur.domain.AgregationMissionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Premier test d'intégration Testcontainers de service-bur (PRD §9 S5) : preuve
 * que la persistance Postgres réelle fonctionne et que le seuil d'agrégation
 * (EF-BUR-04) est respecté de bout en bout, pas seulement en mémoire.
 */
@SpringBootTest
@Testcontainers
class BureauAgregatIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private AgregationMissionsService agregationMissionsService;

    @Test
    void persists_missions_in_postgresql_and_respects_the_aggregation_threshold() {
        String tenantId = "tenant-test-" + System.nanoTime();
        String axeId = "axe-test";

        assertThat(agregationMissionsService.agregatPourAxe(tenantId, axeId).seuilAtteint()).isFalse();

        agregationMissionsService.enregistrerMission(tenantId, axeId);
        agregationMissionsService.enregistrerMission(tenantId, axeId);
        assertThat(agregationMissionsService.agregatPourAxe(tenantId, axeId).seuilAtteint())
                .as("le seuil configuré (3) n'est pas encore atteint après 2 enregistrements")
                .isFalse();

        agregationMissionsService.enregistrerMission(tenantId, axeId);
        var agregat = agregationMissionsService.agregatPourAxe(tenantId, axeId);
        assertThat(agregat.seuilAtteint()).isTrue();
        assertThat(agregat.nombreMissions()).contains(3L);
    }

    @Test
    void two_different_axes_are_counted_independently() {
        String tenantId = "tenant-test-" + System.nanoTime();

        agregationMissionsService.enregistrerMission(tenantId, "axe-a");
        agregationMissionsService.enregistrerMission(tenantId, "axe-a");
        agregationMissionsService.enregistrerMission(tenantId, "axe-a");
        agregationMissionsService.enregistrerMission(tenantId, "axe-b");

        assertThat(agregationMissionsService.agregatPourAxe(tenantId, "axe-a").nombreMissions()).contains(3L);
        assertThat(agregationMissionsService.agregatPourAxe(tenantId, "axe-b").seuilAtteint()).isFalse();
    }
}
