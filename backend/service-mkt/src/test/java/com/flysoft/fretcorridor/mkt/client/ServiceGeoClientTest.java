package com.flysoft.fretcorridor.mkt.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceGeoClientTest {

    private static final String AXES_JSON = """
            [{"id":"%s","hubOrigineVille":"Douala","hubDestinationVille":"Yaoundé",
              "hubOrigineLatitude":4.05,"hubOrigineLongitude":9.7,
              "hubDestinationLatitude":3.87,"hubDestinationLongitude":11.52,"parametres":null}]
            """;

    @Mock private NominatimGeocodingClient nominatimGeocodingClient;

    private RestClient.Builder builder;
    private MockRestServiceServer serveur;
    private ServiceGeoClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        builder = RestClient.builder().baseUrl("http://service-geo");
        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new ServiceGeoClient(builder.build(), nominatimGeocodingClient);
    }

    // Normalisation accents/casse (21/10) : "Yaounde" (saisie courante sans
    // accent) doit matcher le hub "Yaoundé" -- sans ca, une demande
    // legitime basculait inutilement vers le geocodage de repli.
    @Test
    void resoudreAxe_matches_by_name_despite_missing_accents_and_different_case() {
        UUID axeId = UUID.randomUUID();
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        AXES_JSON.formatted(axeId), MediaType.APPLICATION_JSON));

        var resultat = client.resoudreAxe("douala", "YAOUNDE", "tenant-bgft-douala");

        assertThat(resultat).isPresent();
        assertThat(resultat.get().id()).isEqualTo(axeId);
        verify(nominatimGeocodingClient, never()).geocoder(org.mockito.ArgumentMatchers.anyString());
    }

    // Repli geocodage (§1bis, 24/10, GET /api/geo/axes/resoudre livre par le
    // Moteur, PR #151) : aucun hub ne matche par nom -> geocodage Nominatim
    // des deux villes -> resolution par coordonnees.
    @Test
    void resoudreAxe_falls_back_to_geocoding_and_coordinate_resolution_when_no_hub_matches() {
        UUID axeId = UUID.randomUUID();
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));
        when(nominatimGeocodingClient.geocoder("Soa")).thenReturn(Optional.of(new Coordonnees(3.95, 11.55)));
        when(nominatimGeocodingClient.geocoder("Edea")).thenReturn(Optional.of(new Coordonnees(3.8, 10.13)));
        serveur.expect(MockRestRequestMatchers.requestTo(org.hamcrest.Matchers.containsString("/api/geo/axes/resoudre")))
                .andRespond(MockRestResponseCreators.withSuccess(
                        """
                        {"id":"%s","hubOrigineVille":"Yaoundé","hubDestinationVille":"Edéa",
                         "hubOrigineLatitude":3.87,"hubOrigineLongitude":11.52,
                         "hubDestinationLatitude":3.8,"hubDestinationLongitude":10.15,"parametres":null}
                        """.formatted(axeId), MediaType.APPLICATION_JSON));

        var resultat = client.resoudreAxe("Soa", "Edea", "tenant-bgft-douala");

        assertThat(resultat).isPresent();
        assertThat(resultat.get().id()).isEqualTo(axeId);
    }

    @Test
    void resoudreAxe_returns_empty_when_geocoding_fails_for_either_city() {
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));
        when(nominatimGeocodingClient.geocoder("Un-Lieu-Inconnu")).thenReturn(Optional.empty());
        when(nominatimGeocodingClient.geocoder("Douala")).thenReturn(Optional.of(new Coordonnees(4.05, 9.7)));

        assertThat(client.resoudreAxe("Un-Lieu-Inconnu", "Douala", "tenant-bgft-douala")).isEmpty();
    }

    // 204 (aucun axe ne relie ces deux points) -- jamais une exception
    // (ENF-DIS-04), traite comme AXE_NON_DESSERVI par l'appelant.
    @Test
    void resoudreAxe_returns_empty_when_the_resolve_endpoint_returns_no_content() {
        serveur.expect(MockRestRequestMatchers.requestTo("http://service-geo/api/geo/axes/actifs-matching"))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));
        when(nominatimGeocodingClient.geocoder("Kribi")).thenReturn(Optional.of(new Coordonnees(2.95, 9.91)));
        when(nominatimGeocodingClient.geocoder("Garoua")).thenReturn(Optional.of(new Coordonnees(9.3, 13.4)));
        serveur.expect(MockRestRequestMatchers.requestTo(org.hamcrest.Matchers.containsString("/api/geo/axes/resoudre")))
                .andRespond(MockRestResponseCreators.withNoContent());

        assertThat(client.resoudreAxe("Kribi", "Garoua", "tenant-bgft-douala")).isEmpty();
    }
}
