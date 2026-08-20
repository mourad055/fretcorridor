package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.AlerteSeuil;
import com.fretcorridor.bur.domain.Comparateur;
import com.fretcorridor.bur.domain.IndicateurObservatoire;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class AlerteSeuilRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private AlerteSeuilRepositoryAdapter adapter;

    @Test
    void persists_and_lists_alertes_by_tenant() {
        String tenantId = "tenant-test-" + System.nanoTime();
        AlerteSeuil alerte = alerteExemple(tenantId);

        adapter.sauvegarder(alerte);

        assertThat(adapter.listerParTenant(tenantId)).hasSize(1)
                .first()
                .satisfies(a -> {
                    assertThat(a.id()).isEqualTo(alerte.id());
                    assertThat(a.indicateur()).isEqualTo(IndicateurObservatoire.PRIX_MEDIANE);
                    assertThat(a.comparateur()).isEqualTo(Comparateur.SUPERIEUR);
                    assertThat(a.seuil()).isEqualByComparingTo("25000");
                });
    }

    @Test
    void does_not_leak_alertes_across_tenants() {
        String tenantA = "tenant-test-a-" + System.nanoTime();
        String tenantB = "tenant-test-b-" + System.nanoTime();

        adapter.sauvegarder(alerteExemple(tenantA));
        adapter.sauvegarder(alerteExemple(tenantB));

        assertThat(adapter.listerParTenant(tenantA)).hasSize(1).allMatch(a -> a.tenantId().equals(tenantA));
    }

    @Test
    void supprimer_ne_retire_jamais_l_alerte_d_un_autre_tenant() {
        String tenantId = "tenant-test-" + System.nanoTime();
        String autreTenantId = "tenant-test-autre-" + System.nanoTime();
        AlerteSeuil alerte = alerteExemple(tenantId);
        adapter.sauvegarder(alerte);

        adapter.supprimer(alerte.id(), autreTenantId);

        assertThat(adapter.listerParTenant(tenantId)).hasSize(1);
    }

    private AlerteSeuil alerteExemple(String tenantId) {
        return new AlerteSeuil(UUID.randomUUID().toString(), tenantId, UUID.randomUUID(),
                IndicateurObservatoire.PRIX_MEDIANE, Comparateur.SUPERIEUR, new BigDecimal("25000"),
                "actor-bureau-1", Instant.now());
    }
}
