package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.PositionVehicule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PositionRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private PositionRepositoryAdapter adapter;

    @Test
    void persists_and_lists_positions_by_tenant() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID missionId = UUID.randomUUID();

        adapter.enregistrerSiPlusRecente(new PositionVehicule(
                missionId, tenantId, UUID.randomUUID(), 4.05, 9.76, Instant.now()));

        assertThat(adapter.listerParTenant(tenantId)).hasSize(1)
                .first().satisfies(p -> assertThat(p.missionId()).isEqualTo(missionId));
    }

    @Test
    void replaces_the_position_of_the_same_mission_with_a_more_recent_one() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID missionId = UUID.randomUUID();
        Instant t0 = Instant.now();

        adapter.enregistrerSiPlusRecente(new PositionVehicule(missionId, tenantId, UUID.randomUUID(), 4.05, 9.76, t0));
        adapter.enregistrerSiPlusRecente(new PositionVehicule(
                missionId, tenantId, UUID.randomUUID(), 4.10, 9.80, t0.plus(1, ChronoUnit.MINUTES)));

        var positions = adapter.listerParTenant(tenantId);
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).latitude()).isEqualTo(4.10);
    }

    @Test
    void ignores_a_position_older_than_the_one_already_stored_late_kafka_message() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID missionId = UUID.randomUUID();
        Instant t0 = Instant.now();

        adapter.enregistrerSiPlusRecente(new PositionVehicule(
                missionId, tenantId, UUID.randomUUID(), 4.10, 9.80, t0.plus(1, ChronoUnit.MINUTES)));
        // Message Kafka arrivé en retard, horodatage antérieur au précédent.
        adapter.enregistrerSiPlusRecente(new PositionVehicule(missionId, tenantId, UUID.randomUUID(), 4.05, 9.76, t0));

        var positions = adapter.listerParTenant(tenantId);
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).latitude())
                .as("la position la plus récente ne doit jamais être écrasée par un message en retard")
                .isEqualTo(4.10);
    }

    @Test
    void does_not_leak_positions_across_tenants() {
        String tenantA = "tenant-test-a-" + System.nanoTime();
        String tenantB = "tenant-test-b-" + System.nanoTime();

        adapter.enregistrerSiPlusRecente(new PositionVehicule(UUID.randomUUID(), tenantA, UUID.randomUUID(), 4.05, 9.76, Instant.now()));
        adapter.enregistrerSiPlusRecente(new PositionVehicule(UUID.randomUUID(), tenantB, UUID.randomUUID(), 12.13, 15.05, Instant.now()));

        assertThat(adapter.listerParTenant(tenantA)).hasSize(1).allMatch(p -> p.tenantId().equals(tenantA));
    }
}
