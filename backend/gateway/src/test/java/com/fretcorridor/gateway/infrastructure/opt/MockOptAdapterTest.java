package com.fretcorridor.gateway.infrastructure.opt;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockOptAdapterTest {

    private final MockOptAdapter adapter = new MockOptAdapter();

    @Test
    void returns_only_the_missions_of_the_requested_tenant() {
        StepVerifier.create(adapter.listerMissionsParTenant("tenant-bgft-douala").collectList())
                .assertNext(missions -> assertThat(missions)
                        .hasSize(2)
                        .allMatch(m -> m.tenantId().equals("tenant-bgft-douala")))
                .verifyComplete();
    }

    @Test
    void returns_a_different_set_for_a_different_tenant() {
        StepVerifier.create(adapter.listerMissionsParTenant("tenant-bgft-tchad").collectList())
                .assertNext(missions -> assertThat(missions)
                        .hasSize(1)
                        .allMatch(m -> m.tenantId().equals("tenant-bgft-tchad")))
                .verifyComplete();
    }
}
