package com.fretcorridor.opt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Construit le RestClient utilise pour appeler service-geo en synchrone interne.
 *
 * Choix RestClient plutot que RestTemplate (en maintenance, aucune evolution prevue)
 * ou WebClient (reactif - inutile ici, OPT appelle GEO de facon strictement
 * bloquante et sequentielle dans son cycle L0, pas de besoin de non-blocking).
 *
 * API ClientHttpRequestFactorySettings / ClientHttpRequestFactories : package
 * org.springframework.boot.web.client, valable Spring Boot 3.3.x (ce module).
 * A NE PAS confondre avec org.springframework.http.client.ClientHttpRequestFactorySettings
 * + ClientHttpRequestFactoryBuilder, qui n'existent qu'a partir de Boot 3.4 / Framework 6.2.
 *
 * Timeouts geres au niveau de la factory de requetes, pas au niveau de chaque
 * appel individuel : garantit qu'aucun appel ne peut oublier de les definir.
 */
@Configuration
@EnableConfigurationProperties(ServiceGeoClientProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient serviceGeoRestClient(ServiceGeoClientProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
