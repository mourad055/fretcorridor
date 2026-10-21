package com.flysoft.fretcorridor.mkt.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceGeoClientTest {

    private static final String AXES_JSON = """
            [{"id":"%s","hubOrigineVille":"Douala","hubDestinationVille":"Yaoundé",
              "hubOrigineLatitude":4.05,"hubOrigineLongitude":9.7,
              "hubDestinationLatitude":3.87,"hubDestinationLongitude":11.52,"parametres":null}]
            """;

    // Normalisation accents/casse (21/10, suggeree par Personne 3) :
    // "Yaounde" (saisie courante sans accent) doit matcher le hub
    // "Yaoundé" -- sans ca, une demande legitime basculait inutilement
    // vers le geocodage de repli.
    @Test
    void resoudreAxe_matches_despite_missing_accents_and_different_case() {
        UUID axeId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://service-geo");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        AXES_JSON.formatted(axeId), MediaType.APPLICATION_JSON));

        ServiceGeoClient client = new ServiceGeoClient(builder.build());

        var resultat = client.resoudreAxe("douala", "YAOUNDE");

        assertThat(resultat).isPresent();
        assertThat(resultat.get().id()).isEqualTo(axeId);
    }

    @Test
    void resoudreAxe_returns_empty_when_no_hub_matches() {
        UUID axeId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://service-geo");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        AXES_JSON.formatted(axeId), MediaType.APPLICATION_JSON));

        ServiceGeoClient client = new ServiceGeoClient(builder.build());

        assertThat(client.resoudreAxe("Kribi", "Bafoussam")).isEmpty();
    }
}
