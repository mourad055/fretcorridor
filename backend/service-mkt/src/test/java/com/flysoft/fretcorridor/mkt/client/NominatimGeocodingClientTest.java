package com.flysoft.fretcorridor.mkt.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class NominatimGeocodingClientTest {

    private RestClient.Builder builderDe(String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl);
    }

    @Test
    void geocoder_returns_the_coordinates_of_the_first_result() {
        RestClient.Builder builder = builderDe("https://nominatim.openstreetmap.org");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(MockRestRequestMatchers.requestTo(
                        org.hamcrest.Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "[{\"lat\":\"4.0511\",\"lon\":\"9.7679\"}]", MediaType.APPLICATION_JSON));

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder.build());

        var resultat = client.geocoder("Douala");

        assertThat(resultat).isPresent();
        assertThat(resultat.get().latitude()).isEqualTo(4.0511);
        assertThat(resultat.get().longitude()).isEqualTo(9.7679);
        serveur.verify();
    }

    @Test
    void geocoder_returns_empty_when_nominatim_has_no_result() {
        RestClient.Builder builder = builderDe("https://nominatim.openstreetmap.org");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(MockRestRequestMatchers.requestTo(
                        org.hamcrest.Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder.build());

        assertThat(client.geocoder("Un-Lieu-Qui-N-Existe-Pas")).isEmpty();
    }

    @Test
    void geocoder_returns_empty_and_never_throws_when_nominatim_is_unreachable() {
        RestClient.Builder builder = builderDe("https://nominatim.openstreetmap.org");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(MockRestRequestMatchers.requestTo(
                        org.hamcrest.Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withServerError());

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder.build());

        assertThat(client.geocoder("Douala")).isEmpty();
    }

    @Test
    void geocoder_returns_empty_for_a_blank_query_without_any_network_call() {
        RestClient.Builder builder = builderDe("https://nominatim.openstreetmap.org");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        // Aucune attente enregistree : serveur.verify() echouerait si un appel partait.

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder.build());

        assertThat(client.geocoder("   ")).isEmpty();
        assertThat(client.geocoder(null)).isEmpty();
        serveur.verify();
    }

    @Test
    void geocoder_caches_repeated_queries_and_calls_nominatim_only_once() {
        RestClient.Builder builder = builderDe("https://nominatim.openstreetmap.org");
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(org.springframework.test.web.client.ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "[{\"lat\":\"4.0511\",\"lon\":\"9.7679\"}]", MediaType.APPLICATION_JSON));

        NominatimGeocodingClient client = new NominatimGeocodingClient(builder.build());

        assertThat(client.geocoder("Douala")).isPresent();
        assertThat(client.geocoder("douala")).isPresent(); // meme ville, casse differente
        assertThat(client.geocoder(" Douala ")).isPresent(); // espaces superflus

        serveur.verify(); // un seul appel HTTP malgre 3 requetes
    }
}
