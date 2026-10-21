package com.flysoft.fretcorridor.mkt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Geocodage de repli (plan-fretcorridor-reorientation.md §1bis, 21/10) :
 * utilise UNIQUEMENT quand le nom de ville saisi ne matche aucun hub
 * (ServiceGeoClient.resoudreAxe echoue en matching exact). Appele cote
 * backend, jamais depuis l'app mobile -- politique d'usage Nominatim
 * (max 1 req/s, User-Agent identifiant, cache obligatoire, licence ODbL)
 * intenable depuis chaque telephone (IP et debit non maitrisables).
 */
@ConfigurationProperties(prefix = "spring.client.nominatim")
@Validated
public class NominatimClientProperties {

    @NotBlank
    private String baseUrl = "https://nominatim.openstreetmap.org";

    @NotBlank
    private String userAgent = "FretCorridor/1.0 (resolution-axe-publication)";

    @Positive
    private int connectTimeoutMs = 1000;

    @Positive
    private int readTimeoutMs = 2000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
