package com.fretcorridor.gateway.infrastructure.trk;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockTrkAdapterTest {

    private final MockTrkAdapter adapter = new MockTrkAdapter();

    @Test
    void returns_only_the_positions_of_the_requested_tenant() {
        StepVerifier.create(adapter.listerPositionsParTenant("tenant-bgft-douala", "delegation-token-1").collectList())
                .assertNext(positions -> assertThat(positions)
                        .hasSize(2)
                        .allMatch(p -> p.tenantId().equals("tenant-bgft-douala")))
                .verifyComplete();
    }

    @Test
    void returns_a_different_set_for_a_different_tenant() {
        StepVerifier.create(adapter.listerPositionsParTenant("tenant-bnft-ndjamena", "delegation-token-1").collectList())
                .assertNext(positions -> assertThat(positions)
                        .hasSize(2)
                        .allMatch(p -> p.tenantId().equals("tenant-bnft-ndjamena")))
                .verifyComplete();
    }

    @Test
    void every_position_has_a_capture_timestamp_in_the_past() {
        StepVerifier.create(adapter.listerPositionsParTenant("tenant-bgft-douala", "delegation-token-1").collectList())
                .assertNext(positions -> assertThat(positions)
                        .allMatch(p -> p.capturedLe().isBefore(java.time.Instant.now())))
                .verifyComplete();
    }
}
