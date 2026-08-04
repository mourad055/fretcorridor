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
 * Construit les RestClient utilises pour les appels synchrones internes du
 * Moteur (meme porteur) : service-geo (L0) et service-mat (L1).
 *
 * Choix RestClient plutot que RestTemplate (en maintenance) ou WebClient
 * (reactif - inutile ici, appels strictement bloquants et sequentiels dans
 * le cycle L0/L1). Timeouts geres au niveau de la factory de requetes, pas
 * au niveau de chaque appel : garantit qu'aucun appel ne peut oublier de les
 * definir.
 */
@Configuration
@EnableConfigurationProperties({ServiceGeoClientProperties.class, ServiceMatClientProperties.class})
public class RestClientConfig {

    @Bean
    public RestClient serviceGeoRestClient(ServiceGeoClientProperties properties) {
        return construireRestClient(properties.getBaseUrl(),
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    public RestClient serviceMatRestClient(ServiceMatClientProperties properties) {
        return construireRestClient(properties.getBaseUrl(),
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    private RestClient construireRestClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
