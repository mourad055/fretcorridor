package com.fretcorridor.trk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module de suivi et ETA FretCorridor - perimetre Personne 3 (Moteur).
 *
 * Sprint 6 : ingestion tolerante a la connectivite de PositionBrute (Mobile/FLT
 * -> TRK, async Kafka), recalcul dynamique de l'ETA avec intervalle de confiance,
 * detection d'anomalies (EF-TRK-01/02/03/04). Dependances internes synchrones :
 * GEO (referentiel axes/hubs pour l'ecart de trajectoire), OPT (itineraire
 * retenu pour le calcul d'ETA).
 *
 * Aucune interface directe - service backend pur, sans exposition Mobile/Web.
 */
@SpringBootApplication
public class ServiceTrkApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceTrkApplication.class, args);
    }
}
