package com.fretcorridor.opt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes typees pour l'appel HTTP synchrone interne vers service-geo
 * (spring.client.service-geo.* dans application.yml / application-docker.yml).
 *
 * @Validated + contraintes Bean Validation : si une valeur manque ou est invalide
 * au demarrage (ex. timeout negatif), le service refuse de demarrer plutot que
 * de tourner avec une config silencieusement cassee - fail-fast intentionnel.
 */
@ConfigurationProperties(prefix = "spring.client.service-geo")
@Validated
public class ServiceGeoClientProperties {

    @NotBlank
    private String baseUrl;

    @Positive
    private int connectTimeoutMs = 200;

    @Positive
    private int readTimeoutMs = 300;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
