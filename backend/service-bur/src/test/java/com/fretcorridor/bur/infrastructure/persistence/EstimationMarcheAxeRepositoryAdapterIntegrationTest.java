package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.EstimationMarcheAxe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EstimationMarcheAxeRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private EstimationMarcheAxeRepositoryAdapter adapter;

    @Test
    void persists_and_retrieves_an_estimation_by_tenant_and_axe() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID axeId = UUID.randomUUID();
        EstimationMarcheAxe estimation = new EstimationMarcheAxe(tenantId, axeId, new BigDecimal("350"),
                "enquête terrain Q1 2026", "actor-bureau-1", Instant.parse("2026-01-15T10:00:00Z"));

        adapter.definir(estimation);

        Optional<EstimationMarcheAxe> relue = adapter.pour(tenantId, axeId);
        assertThat(relue).isPresent();
        assertThat(relue.get().volumeMensuelEstime()).isEqualByComparingTo("350");
        assertThat(relue.get().source()).isEqualTo("enquête terrain Q1 2026");
        assertThat(relue.get().definieParActeurId()).isEqualTo("actor-bureau-1");
    }

    @Test
    void redefining_an_estimation_replaces_the_previous_one() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID axeId = UUID.randomUUID();
        adapter.definir(new EstimationMarcheAxe(tenantId, axeId, new BigDecimal("350"), "enquête V1",
                "actor-bureau-1", Instant.parse("2026-01-15T10:00:00Z")));

        adapter.definir(new EstimationMarcheAxe(tenantId, axeId, new BigDecimal("400"), "enquête V2",
                "actor-bureau-2", Instant.parse("2026-04-01T10:00:00Z")));

        Optional<EstimationMarcheAxe> relue = adapter.pour(tenantId, axeId);
        assertThat(relue).isPresent();
        assertThat(relue.get().volumeMensuelEstime()).isEqualByComparingTo("400");
        assertThat(relue.get().source()).isEqualTo("enquête V2");
    }

    @Test
    void returns_empty_when_no_estimation_exists_for_the_axe() {
        assertThat(adapter.pour("tenant-inconnu", UUID.randomUUID())).isEmpty();
    }

    @Test
    void does_not_leak_estimations_across_tenants_on_the_same_axe() {
        UUID axeId = UUID.randomUUID();
        String tenantA = "tenant-test-a-" + System.nanoTime();
        String tenantB = "tenant-test-b-" + System.nanoTime();
        adapter.definir(new EstimationMarcheAxe(tenantA, axeId, new BigDecimal("350"), "enquête A",
                "actor-bureau-1", Instant.now()));

        assertThat(adapter.pour(tenantB, axeId)).isEmpty();
    }
}
