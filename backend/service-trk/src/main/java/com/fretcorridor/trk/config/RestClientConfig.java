package com.fretcorridor.trk.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Construit le RestClient utilise pour l'appel de TRK vers service-opt
 * (recuperation origine/destination pour le calcul d'ETA). Meme construction
 * que RestClientConfig cote service-opt, pour rester coherent entre les
 * services du perimetre Moteur.
 */
@Configuration
@EnableConfigurationProperties(ServiceOptClientProperties.class)
public class RestClientConfig {

    @Bean
    @Qualifier("serviceOptRestClient")
    public RestClient serviceOptRestClient(ServiceOptClientProperties properties) {
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
