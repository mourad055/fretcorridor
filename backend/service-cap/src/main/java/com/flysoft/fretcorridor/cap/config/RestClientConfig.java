package com.flysoft.fretcorridor.cap.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({ServiceFltClientProperties.class, ServiceGeoClientProperties.class, ServiceNotClientProperties.class, ServiceOptClientProperties.class})
public class RestClientConfig {

    @Bean
    @Qualifier("serviceFltRestClient")
    public RestClient serviceFltRestClient(ServiceFltClientProperties properties) {
        return client(properties.getBaseUrl(), properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("serviceOptRestClient")
    public RestClient serviceOptRestClient(ServiceOptClientProperties properties) {
        return client(properties.getBaseUrl(), properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("serviceGeoRestClient")
    public RestClient serviceGeoRestClient(ServiceGeoClientProperties properties) {
        return client(properties.getBaseUrl(), properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    @Bean
    @Qualifier("serviceNotRestClient")
    public RestClient serviceNotRestClient(ServiceNotClientProperties properties) {
        return client(properties.getBaseUrl(), properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
    }

    private RestClient client(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
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
