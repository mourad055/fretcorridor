package com.fretcorridor.bur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Squelette hexagonal (Sprint 3, PRD §9 S3). Aucun endpoint réel n'est encore
 * exposé — le premier endpoint réel (agrégat simple) arrive au Sprint 5, avec
 * son premier test d'intégration Testcontainers.
 */
@SpringBootApplication
@EnableScheduling
public class ServiceBurApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceBurApplication.class, args);
    }
}
