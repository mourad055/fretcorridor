package com.fretcorridor.gateway.infrastructure.auth;

import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class MockIdaAuthenticationAdapterTest {

    private final MockIdaAuthenticationAdapter adapter = new MockIdaAuthenticationAdapter();

    @Test
    void authenticates_a_known_actor_with_the_dev_code() {
        StepVerifier.create(adapter.authenticate("+237600000001", "123456"))
                .expectNextMatches(actor -> actor.role() == Role.BUREAU
                        && actor.tenantId().equals("tenant-bgft-douala"))
                .verifyComplete();
    }

    @Test
    void rejects_an_unknown_phone_number() {
        StepVerifier.create(adapter.authenticate("+237699999999", "123456"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void rejects_a_wrong_code() {
        StepVerifier.create(adapter.authenticate("+237600000001", "000000"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }
}
