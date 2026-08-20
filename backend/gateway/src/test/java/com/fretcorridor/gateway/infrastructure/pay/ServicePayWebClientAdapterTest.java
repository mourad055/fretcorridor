package com.fretcorridor.gateway.infrastructure.pay;

import com.fretcorridor.gateway.domain.pay.PayServiceIndisponibleException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ServicePayWebClientAdapterTest {

    private MockWebServer servicePay;
    private ServicePayWebClientAdapter adapter;

    @BeforeEach
    void demarrerServicePayFactice() throws IOException {
        servicePay = new MockWebServer();
        servicePay.start();
        adapter = new ServicePayWebClientAdapter(WebClient.builder(), servicePay.url("/").toString());
    }

    @AfterEach
    void arreterServicePayFactice() throws IOException {
        servicePay.shutdown();
    }

    @Test
    void reads_the_tenant_report_with_the_delegation_token() throws InterruptedException {
        servicePay.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        StepVerifier.create(adapter.rapportDuTenant("tenant-bgft-douala", "delegation-token-1"))
                .verifyComplete();

        var requete = servicePay.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/v1/pay/tenants/tenant-bgft-douala/rapport");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void refuses_to_call_service_pay_without_a_delegation_token() {
        StepVerifier.create(adapter.rapportDuTenant("tenant-bgft-douala", null))
                .expectError(PayServiceIndisponibleException.class)
                .verify();

        StepVerifier.create(adapter.ecrituresDuTransporteur("transporteur-1", null))
                .expectError(PayServiceIndisponibleException.class)
                .verify();

        StepVerifier.create(adapter.paiementsEspecesDuTenant("tenant-bgft-douala", null))
                .expectError(PayServiceIndisponibleException.class)
                .verify();

        StepVerifier.create(adapter.modePaiementChoisi("mission-1", null))
                .expectError(PayServiceIndisponibleException.class)
                .verify();

        assertThat(servicePay.getRequestCount()).isZero();
    }
}
