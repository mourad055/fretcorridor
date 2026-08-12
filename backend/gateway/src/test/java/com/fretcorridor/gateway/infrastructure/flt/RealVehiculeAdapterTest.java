package com.fretcorridor.gateway.infrastructure.flt;

import com.fretcorridor.gateway.domain.flt.DeclarationVehicule;
import com.fretcorridor.gateway.domain.flt.FltServiceIndisponibleException;
import com.fretcorridor.gateway.domain.flt.VehiculeRefuseException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RealVehiculeAdapterTest {

    private MockWebServer serviceFlt;
    private RealVehiculeAdapter adapter;

    @BeforeEach
    void demarrerServiceFltFactice() throws IOException {
        serviceFlt = new MockWebServer();
        serviceFlt.start();
        adapter = new RealVehiculeAdapter(WebClient.builder(), serviceFlt.url("/").toString());
    }

    @AfterEach
    void arreterServiceFltFactice() throws IOException {
        serviceFlt.shutdown();
    }

    private DeclarationVehicule declaration() {
        return new DeclarationVehicule("Camion 10T", "LT 1234 AB", null, null, null, null, null, null, false);
    }

    @Test
    void declares_a_vehicle_with_the_delegation_token() throws InterruptedException {
        serviceFlt.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":"v1","typeVehicule":"Camion 10T","immatriculation":"LT 1234 AB","profilMatieresDangereuses":false,"dateCreation":"2026-08-12T10:00:00"}
                        """));

        StepVerifier.create(adapter.declarer("delegation-token-1", declaration()))
                .expectNextMatches(v -> v.id().equals("v1") && v.typeVehicule().equals("Camion 10T"))
                .verifyComplete();

        var requete = serviceFlt.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/flt/vehicules");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void lists_my_vehicles() throws InterruptedException {
        serviceFlt.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"id":"v1","typeVehicule":"Camion 10T","profilMatieresDangereuses":false,"dateCreation":"2026-08-12T10:00:00"}]
                        """));

        StepVerifier.create(adapter.mesVehicules("delegation-token-1"))
                .expectNextMatches(v -> v.id().equals("v1"))
                .verifyComplete();

        var requete = serviceFlt.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/flt/vehicules/mes");
    }

    @Test
    void maps_a_400_to_a_refused_error() {
        serviceFlt.enqueue(new MockResponse().setResponseCode(400).setBody("TYPE_VEHICULE_MANQUANT"));

        StepVerifier.create(adapter.declarer("delegation-token-1", declaration()))
                .expectError(VehiculeRefuseException.class)
                .verify();
    }

    @Test
    void refuses_to_call_service_flt_without_a_delegation_token() {
        StepVerifier.create(adapter.declarer(null, declaration()))
                .expectError(FltServiceIndisponibleException.class)
                .verify();

        assertThat(serviceFlt.getRequestCount()).isZero();
    }
}
