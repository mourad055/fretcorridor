package com.fretcorridor.gateway.infrastructure.kyc;

import com.fretcorridor.gateway.domain.kyc.DecisionInvalideException;
import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.domain.kyc.KycStatut;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockIdaKycAdapterTest {

    private final MockIdaKycAdapter adapter = new MockIdaKycAdapter();

    @Test
    void lists_three_seeded_pending_dossiers() {
        StepVerifier.create(adapter.listerEnAttente().collectList())
                .assertNext(dossiers -> assertThat(dossiers).hasSize(3))
                .verifyComplete();
    }

    @Test
    void deciding_removes_the_dossier_from_the_pending_list() {
        adapter.decider("kyc-1", KycStatut.VALIDE, UUID.randomUUID().toString()).block();

        StepVerifier.create(adapter.listerEnAttente().collectList())
                .assertNext(dossiers -> assertThat(dossiers).hasSize(2)
                        .noneMatch(d -> d.id().equals("kyc-1")))
                .verifyComplete();
    }

    @Test
    void rejects_a_decision_that_reopens_a_dossier_to_pending() {
        StepVerifier.create(adapter.decider("kyc-1", KycStatut.EN_ATTENTE, UUID.randomUUID().toString()))
                .expectError(DecisionInvalideException.class)
                .verify();
    }

    @Test
    void rejects_deciding_an_unknown_dossier() {
        StepVerifier.create(adapter.decider("kyc-inconnu", KycStatut.VALIDE, UUID.randomUUID().toString()))
                .expectError(KycDossierIntrouvableException.class)
                .verify();
    }

    @Test
    void replaying_the_same_idempotency_key_returns_the_same_result_without_reapplying() {
        String key = UUID.randomUUID().toString();

        var first = adapter.decider("kyc-2", KycStatut.REJETE, key).block();
        var replay = adapter.decider("kyc-2", KycStatut.VALIDE, key).block();

        assertThat(replay).isEqualTo(first);
        assertThat(replay.statut()).isEqualTo(KycStatut.REJETE);
    }
}
