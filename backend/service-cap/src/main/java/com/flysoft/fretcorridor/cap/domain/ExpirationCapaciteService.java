package com.flysoft.fretcorridor.cap.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** EF-CAP-08 : expiration automatique a l'heure de depart, sans intervention manuelle. */
@Service
public class ExpirationCapaciteService {

    private static final Logger log = LoggerFactory.getLogger(ExpirationCapaciteService.class);

    private final CapaciteRepository capaciteRepository;

    public ExpirationCapaciteService(CapaciteRepository capaciteRepository) {
        this.capaciteRepository = capaciteRepository;
    }

    @Scheduled(fixedDelayString = "${fretcorridor.cap.expiration-interval-ms:60000}")
    @Transactional
    public void expirerCapacitesDepassees() {
        List<Capacite> capacitesAExpirer = capaciteRepository.findByDateDepartBeforeAndExpireeFalse(Instant.now());

        if (capacitesAExpirer.isEmpty()) {
            return;
        }

        capacitesAExpirer.forEach(Capacite::marquerExpiree);
        capaciteRepository.saveAll(capacitesAExpirer);

        log.info("{} capacite(s) expiree(s) automatiquement (fenetre de depart depassee).",
                capacitesAExpirer.size());
    }
}
