package com.fretcorridor.opt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes typees pour l'appel HTTP vers le service d'itineraires Valhalla
 * (spring.client.valhalla.* dans application.yml / application-docker.yml).
 *
 * DECISION D'EQUIPE (ADR a consigner dans docs/, pas une exigence du CDC) :
 * le CDC S8.10 ne fixe aucun budget de latence explicite pour l'appel
 * Valhalla lui-meme - seuls Filtrage L0 (10/50ms), Cycle L1 (1s/5s),
 * Sequencement L2 (5s/30s), Oracle L3 (50ms/200ms avec cache, jusqu'a 2s
 * sans), Tarification L4 (20ms/100ms) et Estimation prix client (200ms/1s)
 * y figurent. On assimile ici Valhalla au budget Oracle L3 (profil similaire :
 * appel externe cacheable) - extrapolation raisonnable, PAS une exigence
 * citee du CDC. readTimeoutMs=2000 reprend le plafond "sans cache" de L3
 * comme majorant prudent en attendant un cache Redis sur les trajets frequents.
 */
@ConfigurationProperties(prefix = "spring.client.valhalla")
@Validated
public class ValhallaClientProperties {

    @NotBlank
    private String baseUrl;

    @Positive
    private int connectTimeoutMs = 300;

    @Positive
    private int readTimeoutMs = 2000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
