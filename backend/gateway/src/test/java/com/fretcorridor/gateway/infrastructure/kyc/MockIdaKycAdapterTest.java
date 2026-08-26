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
        StepVerifier.create(adapter.listerEnAttente("tenant-bgft-douala", "tok").collectList())
                .assertNext(dossiers -> assertThat(dossiers).hasSize(3))
                .verifyComplete();
    }

    @Test
    void lists_validated_niveau_2() {
        StepVerifier.create(adapter.listerParNiveau("tenant-bgft-douala", "NIVEAU_2", "tok").collectList())
                .assertNext(dossiers -> assertThat(dossiers).hasSize(1)
                        .allMatch(d -> "NIVEAU_2".equals(d.niveauKyc())))
                .verifyComplete();
    }

    @Test
    void detail_returns_presigned_piece_urls() {
        StepVerifier.create(adapter.detail("kyc-1", "tenant-bgft-douala", "tok"))
                .assertNext(detail -> {
                    assertThat(detail.telephone()).isEqualTo("+237677000001");
                    assertThat(detail.pieces()).hasSize(1);
                    assertThat(detail.pieces().get(0).url()).contains("cni-kyc-1");
                })
                .verifyComplete();
    }

    @Test
    void deciding_removes_the_dossier_from_the_pending_list() {
        adapter.decider("kyc-1", KycStatut.VALIDE, UUID.randomUUID().toString(), "t", "tok", null).block();

        StepVerifier.create(adapter.listerEnAttente("t", "tok").collectList())
                .assertNext(dossiers -> assertThat(dossiers).hasSize(2)
                        .noneMatch(d -> d.id().equals("kyc-1")))
                .verifyComplete();
    }

    @Test
    void rejects_a_decision_that_reopens_a_dossier_to_pending() {
        StepVerifier.create(adapter.decider("kyc-1", KycStatut.EN_ATTENTE, UUID.randomUUID().toString(), "t", "tok", null))
                .expectError(DecisionInvalideException.class)
                .verify();
    }

    @Test
    void rejects_deciding_an_unknown_dossier() {
        StepVerifier.create(adapter.decider("kyc-inconnu", KycStatut.VALIDE, UUID.randomUUID().toString(), "t", "tok", null))
                .expectError(KycDossierIntrouvableException.class)
                .verify();
    }

    @Test
    void replaying_the_same_idempotency_key_returns_the_same_result_without_reapplying() {
        String key = UUID.randomUUID().toString();

        var first = adapter.decider("kyc-2", KycStatut.REJETE, key, "t", "tok", "doublon").block();
        var replay = adapter.decider("kyc-2", KycStatut.VALIDE, key, "t", "tok", null).block();

        assertThat(replay).isEqualTo(first);
        assertThat(replay.statut()).isEqualTo(KycStatut.REJETE);
    }
}
