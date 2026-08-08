package com.fretcorridor.geo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree du microservice service-geo.
 * Referentiel geospatial : axes, hubs, zonage H3 (cf Plan d'execution S4.1, CDC S13).
 * Service autonome, deploye et scale independamment des autres microservices du Moteur.
 */
@SpringBootApplication
public class ServiceGeoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceGeoApplication.class, args);
    }
}
