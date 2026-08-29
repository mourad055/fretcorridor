package com.fretcorridor.opt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Proprietes typees pour l'appel HTTP synchrone interne vers service-trk
 * (spring.client.service-trk.* dans application.yml / application-docker.yml).
 * Meme pattern que ServiceGeoClientProperties/ServiceMatClientProperties.
 *
 * positionMaxAgeSecondes : HYPOTHESE D'EQUIPE (a valider, pas une valeur du
 * plan de reorientation qui ne tranche pas ce point) - une position temps
 * reel plus vieille que ce seuil est ignoree, repli sur la position declaree
 * (CapaciteEnAttente.getPosition()). Meme raisonnement que
 * rayonAppariementKm/detourMaxDistanceKm ailleurs dans le code : jamais un
 * defaut invente sans le documenter comme choix explicite.
 */
@ConfigurationProperties(prefix = "spring.client.service-trk")
@Validated
public class ServiceTrkClientProperties {

    @NotBlank
    private String baseUrl;

    @Positive
    private int connectTimeoutMs = 200;

    @Positive
    private int readTimeoutMs = 300;

    @Positive
    private long positionMaxAgeSecondes = 300; // 5 min par defaut - a valider en equipe

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public long getPositionMaxAgeSecondes() { return positionMaxAgeSecondes; }
    public void setPositionMaxAgeSecondes(long positionMaxAgeSecondes) { this.positionMaxAgeSecondes = positionMaxAgeSecondes; }
}
