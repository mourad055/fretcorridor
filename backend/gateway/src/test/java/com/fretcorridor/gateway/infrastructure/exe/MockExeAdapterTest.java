package com.fretcorridor.gateway.infrastructure.exe;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockExeAdapterTest {

    private final MockExeAdapter adapter = new MockExeAdapter();

    @Test
    void tenant_filter_returns_only_its_own_missions() {
        StepVerifier.create(adapter.listerMissionsParTenant("tenant-bgft-douala", "token").collectList())
                .assertNext(missions -> assertThat(missions)
                        .hasSize(2)
                        .allMatch(m -> m.tenantId().equals("tenant-bgft-douala")))
                .verifyComplete();
    }

    @Test
    void transporteur_filter_returns_only_its_own_mission() {
        StepVerifier.create(adapter.listerMissionsParTransporteur("tenant-bgft-douala",
                        "a0000000-0000-0000-0000-000000000002", "token").collectList())
                .assertNext(missions -> assertThat(missions)
                        .hasSize(1)
                        .allMatch(m -> m.transporteurId().equals("a0000000-0000-0000-0000-000000000002")))
                .verifyComplete();
    }

    @Test
    void each_mission_carries_an_ordered_chronology_of_steps() {
        StepVerifier.create(adapter.listerMissionsParTransporteur("tenant-bgft-douala",
                        "a0000000-0000-0000-0000-000000000002", "token").collectList())
                .assertNext(missions -> assertThat(missions.get(0).etapes())
                        .hasSize(2)
                        .extracting(e -> e.rang())
                        .containsExactly(1, 2))
                .verifyComplete();
    }
}
