package com.fretcorridor.gateway.infrastructure.rest.flt;

import com.fretcorridor.gateway.domain.flt.Vehicule;
import com.fretcorridor.gateway.domain.flt.VehiculePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class VehiculeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VehiculePort vehiculePort;

    private String tokenFor(String phone) {
        return webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\": \"" + phone + "\", \"code\": \"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody()
                .blockFirst()
                .get("token")
                .toString();
    }

    @Test
    void an_authenticated_actor_can_declare_a_vehicle() {
        String token = tokenFor("+237600000002");
        when(vehiculePort.declarer(eq("mock-ida-delegation-token"), any()))
                .thenReturn(Mono.just(new Vehicule("v1", "Camion 10T", "LT 1234 AB", null, null, null, null, null, null, false, "2026-08-12T10:00:00")));

        webTestClient.post().uri("/api/v1/vehicules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"typeVehicule\": \"Camion 10T\", \"immatriculation\": \"LT 1234 AB\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("v1");
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/vehicules/mes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void lists_my_vehicles() {
        String token = tokenFor("+237600000002");
        when(vehiculePort.mesVehicules("mock-ida-delegation-token"))
                .thenReturn(Flux.just(new Vehicule("v1", "Camion 10T", null, null, null, null, null, null, null, false, "2026-08-12T10:00:00")));

        webTestClient.get().uri("/api/v1/vehicules/mes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("v1");

        verify(vehiculePort).mesVehicules("mock-ida-delegation-token");
    }
}
