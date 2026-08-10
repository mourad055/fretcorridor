package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.MissionAppariee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Preuve que la persistance Postgres réelle fonctionne (même principe que
 * BureauAgregatIntegrationTest) — sert aussi de premier test à faire
 * démarrer le contexte Spring complet avec @EnableKafka câblé (aucun broker
 * réel requis : les conteneurs d'écoute Spring Kafka se connectent en
 * arrière-plan, sans bloquer le démarrage du contexte).
 */
@SpringBootTest
@Testcontainers
class MissionAppparieeRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MissionAppparieeRepositoryAdapter adapter;

    @Test
    void persists_and_lists_missions_by_tenant() {
        String tenantId = "tenant-test-" + System.nanoTime();
        MissionAppariee mission = missionExemple(tenantId);

        adapter.enregistrer(mission, UUID.randomUUID());

        assertThat(adapter.listerParTenant(tenantId)).hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.missionId()).isEqualTo(mission.missionId());
                    assertThat(m.origineNom()).isEqualTo("Douala");
                });
    }

    @Test
    void does_not_leak_missions_across_tenants() {
        String tenantA = "tenant-test-a-" + System.nanoTime();
        String tenantB = "tenant-test-b-" + System.nanoTime();

        adapter.enregistrer(missionExemple(tenantA), UUID.randomUUID());
        adapter.enregistrer(missionExemple(tenantB), UUID.randomUUID());

        assertThat(adapter.listerParTenant(tenantA)).hasSize(1).allMatch(m -> m.tenantId().equals(tenantA));
    }

    @Test
    void rejects_a_duplicate_event_id_idempotence() {
        String tenantId = "tenant-test-" + System.nanoTime();
        UUID eventId = UUID.randomUUID();
        adapter.enregistrer(missionExemple(tenantId), eventId);

        assertThatThrownBy(() -> adapter.enregistrer(missionExemple(tenantId), eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MissionAppariee missionExemple(String tenantId) {
        return new MissionAppariee(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "Douala", "Yaoundé", new BigDecimal("50000"), "XAF", Instant.now());
    }
}
