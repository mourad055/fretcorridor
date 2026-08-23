package com.fretcorridor.opt.config;

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
 * Construit les RestClient utilises pour les appels du Moteur :
 * service-geo (L0, synchrone interne), service-mat (L1, synchrone interne),
 * Valhalla (itineraires, integration externe au meme titre que OSRM/OpenStreetMap
 * cf Plan d'execution S1).
 *
 * Trois beans du meme type RestClient : @Qualifier explicite sur chaque bean
 * ET sur chaque point d'injection (ServiceGeoClient, ServiceMatClient,
 * ValhallaClient) - Spring resoudrait par nom de parametre sans lui, mais un
 * qualifier explicite est plus sur (survit a un renommage accidentel de
 * parametre) et plus lisible.
 */
@Configuration
@EnableConfigurationProperties({
        ServiceGeoClientProperties.class,
        ServiceMatClientProperties.class,
        ServiceCapClientProperties.class,
        ValhallaClientProperties.class
})
public class RestClientConfig {

    @Bean
    @Qualifier("serviceGeoRestClient")
    public RestClient serviceGeoRestClient(ServiceGeoClientProperties properties) {
        return construireRestClient(properties.getBaseUrl(),
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("serviceMatRestClient")
    public RestClient serviceMatRestClient(ServiceMatClientProperties properties) {
        return construireRestClient(properties.getBaseUrl(),
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("serviceCapRestClient")
    public RestClient serviceCapRestClient(ServiceCapClientProperties properties) {
        return construireRestClient(properties.getBaseUrl(),
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("valhallaRestClient")
    public RestClient valhallaRestClient(ValhallaClientProperties properties) {
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
