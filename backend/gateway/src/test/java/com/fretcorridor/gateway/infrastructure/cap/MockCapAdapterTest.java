package com.fretcorridor.gateway.infrastructure.cap;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MockCapAdapterTest {

    private final MockCapAdapter adapter = new MockCapAdapter();

    @Test
    void returns_only_the_capacities_of_the_requested_transporteur() {
        StepVerifier.create(adapter.listerParTransporteur("actor-transporteur-1").collectList())
                .assertNext(capacites -> assertThat(capacites)
                        .hasSize(2)
                        .allMatch(c -> c.transporteurId().equals("actor-transporteur-1")))
                .verifyComplete();
    }

    @Test
    void returns_a_different_set_for_a_different_transporteur() {
        StepVerifier.create(adapter.listerParTransporteur("actor-transporteur-2").collectList())
                .assertNext(capacites -> assertThat(capacites)
                        .hasSize(1)
                        .allMatch(c -> c.transporteurId().equals("actor-transporteur-2")))
                .verifyComplete();
    }

    @Test
    void returns_nothing_for_an_unknown_transporteur() {
        StepVerifier.create(adapter.listerParTransporteur("actor-inconnu").collectList())
                .assertNext(capacites -> assertThat(capacites).isEmpty())
                .verifyComplete();
    }
}
