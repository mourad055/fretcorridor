package com.fretcorridor.gateway.infrastructure.not;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockNotAdapterTest {

    private final MockNotAdapter adapter = new MockNotAdapter();

    @Test
    void tenant_filter_returns_only_its_own_notifications() {
        StepVerifier.create(adapter.listerNotificationsParTenant("tenant-bgft-douala").collectList())
                .assertNext(notifications -> assertThat(notifications)
                        .hasSize(2)
                        .allMatch(n -> n.tenantId().equals("tenant-bgft-douala")))
                .verifyComplete();
    }

    @Test
    void a_tenant_with_no_notification_gets_an_empty_list() {
        StepVerifier.create(adapter.listerNotificationsParTenant("tenant-inconnu").collectList())
                .assertNext(notifications -> assertThat(notifications).isEmpty())
                .verifyComplete();
    }
}
