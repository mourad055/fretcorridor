package com.fretcorridor.opt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes typees pour l'appel HTTP synchrone interne vers service-mat
 * (spring.client.service-mat.* dans application.yml / application-docker.yml).
 * Meme pattern que ServiceGeoClientProperties - fail-fast au demarrage si
 * une valeur manque ou est invalide.
 */
@ConfigurationProperties(prefix = "spring.client.service-mat")
@Validated
public class ServiceMatClientProperties {

    @NotBlank
    private String baseUrl;

    @Positive
    private int connectTimeoutMs = 200;

    @Positive
    private int readTimeoutMs = 300;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
