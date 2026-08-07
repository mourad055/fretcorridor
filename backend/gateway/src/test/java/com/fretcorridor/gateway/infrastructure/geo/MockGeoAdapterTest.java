package com.fretcorridor.gateway.infrastructure.geo;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockGeoAdapterTest {

    private final MockGeoAdapter adapter = new MockGeoAdapter();

    @Test
    void returns_only_the_axes_of_the_requested_tenant() {
        StepVerifier.create(adapter.listerAxesParTenant("tenant-bgft-douala").collectList())
                .assertNext(axes -> assertThat(axes).hasSize(2).allMatch(a -> a.tenantId().equals("tenant-bgft-douala")))
                .verifyComplete();
    }

    @Test
    void returns_a_different_set_for_a_different_tenant() {
        StepVerifier.create(adapter.listerAxesParTenant("tenant-bnft-ndjamena").collectList())
                .assertNext(axes -> assertThat(axes).hasSize(1).allMatch(a -> a.tenantId().equals("tenant-bnft-ndjamena")))
                .verifyComplete();
    }

    @Test
    void returns_nothing_for_an_unknown_tenant() {
        StepVerifier.create(adapter.listerAxesParTenant("tenant-inconnu").collectList())
                .assertNext(axes -> assertThat(axes).isEmpty())
                .verifyComplete();
    }
}
