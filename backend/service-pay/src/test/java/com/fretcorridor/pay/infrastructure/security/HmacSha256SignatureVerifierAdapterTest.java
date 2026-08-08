package com.fretcorridor.pay.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSha256SignatureVerifierAdapterTest {

    private final HmacSha256SignatureVerifierAdapter adapter = new HmacSha256SignatureVerifierAdapter("secret-de-test-min-32-caracteres");

    @Test
    void accepts_a_signature_computed_with_the_same_secret() throws Exception {
        String corps = "{\"missionId\":\"mission-1\"}";
        String signatureAttendue = hmac(corps, "secret-de-test-min-32-caracteres");

        assertThat(adapter.estValide(corps, signatureAttendue)).isTrue();
    }

    @Test
    void rejects_a_signature_computed_with_a_different_secret() throws Exception {
        String corps = "{\"missionId\":\"mission-1\"}";
        String signatureForgee = hmac(corps, "un-autre-secret-que-lattaquant-connait");

        assertThat(adapter.estValide(corps, signatureForgee)).isFalse();
    }

    @Test
    void rejects_a_correct_signature_computed_over_a_tampered_body() throws Exception {
        String corpsSigne = "{\"missionId\":\"mission-1\",\"montant\":100}";
        String signature = hmac(corpsSigne, "secret-de-test-min-32-caracteres");
        String corpsAlteré = "{\"missionId\":\"mission-1\",\"montant\":100000}";

        assertThat(adapter.estValide(corpsAlteré, signature)).isFalse();
    }

    @Test
    void rejects_a_blank_signature() {
        assertThat(adapter.estValide("{}", "")).isFalse();
    }

    private static String hmac(String corps, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(corps.getBytes(StandardCharsets.UTF_8)));
    }
}
