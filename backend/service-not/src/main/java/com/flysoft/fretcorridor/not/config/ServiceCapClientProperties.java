package com.flysoft.fretcorridor.not.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.client.service-cap")
@Validated
public class ServiceCapClientProperties {

    @NotBlank
    private String baseUrl;

    // Marge raisonnable dès le départ (cf. ServiceFltClientProperties côté
    // service-cap, qui avait démarré trop serré à 200/300ms et a dû être
    // corrigé après un test réel — pas la peine de reproduire l'erreur ici).
    @Positive
    private int connectTimeoutMs = 500;

    @Positive
    private int readTimeoutMs = 1000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
