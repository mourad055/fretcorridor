package com.fretcorridor.opt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes typees pour l'appel HTTP synchrone interne vers service-not
 * (spring.client.service-not.* dans application.yml / application-docker.yml).
 * Meme pattern que ServiceCapClientProperties.
 */
@ConfigurationProperties(prefix = "spring.client.service-not")
@Validated
public class ServiceNotClientProperties {

    @NotBlank
    private String baseUrl;

    @Positive
    private int connectTimeoutMs = 300;

    @Positive
    private int readTimeoutMs = 500;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
