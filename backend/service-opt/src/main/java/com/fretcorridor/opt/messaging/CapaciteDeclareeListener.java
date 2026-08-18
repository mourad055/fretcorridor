package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme CapaciteDeclaree (service-cap, Mobile) et met en attente pour le
 * prochain cycle de matching (MatchingCycleService) - jamais de matching
 * immediat, conforme a EF-MAT-01 ("par cycles a fenetre").
 */
@Component
public class CapaciteDeclareeListener {

    private static final Logger log = LoggerFactory.getLogger(CapaciteDeclareeListener.class);

    private final CapaciteEnAttenteRepository repository;

    public CapaciteDeclareeListener(CapaciteEnAttenteRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "capacite-declaree", containerFactory = "capaciteDeclareeKafkaListenerContainerFactory")
    public void ingerer(CapaciteDeclareeEvent event) {
        try {
            repository.save(new CapaciteEnAttente(
                    event.capaciteId(), event.axeId(), event.transporteurId(), event.vehiculeId(),
                    event.eventId(), event.valeursCriteres(),
                    event.position(), event.profilCamion(), event.typeVehicule(),
                    event.capaciteResiduelleKg(), event.volumeResiduelM3()));
            log.debug("Capacite mise en attente - capacite={}, axe={}, eventId={}",
                    event.capaciteId(), event.axeId(), event.eventId());
        } catch (DataIntegrityViolationException violation) {
            // SQLState 23505 (unique_violation sur event_id) = doublon Kafka
            // (redelivrance), benin, ignore comme prevu. Tout autre SQLState
            // (ex. 23502 NOT NULL sur capacite_residuelle_kg si service-cap
            // publie un champ requis manquant) est une VRAIE perte de donnee -
            // ne doit jamais etre confondu avec un doublon (bug corrige suite
            // a revue croisee avec service-cap, 2026-08-17).
            String sqlState = extraireSqlState(violation);
            if ("23505".equals(sqlState)) {
                log.info("CapaciteDeclaree deja ingeree, doublon ignore - eventId={}", event.eventId());
            } else {
                log.error("Echec ingestion CapaciteDeclaree (contrainte violee, SQLState={}) - "
                        + "capacite NON enregistree, capacite={}, eventId={} - a investiguer cote "
                        + "service-cap (champ requis manquant ?)",
                        sqlState, event.capaciteId(), event.eventId(), violation);
            }
        }
    }

    private static String extraireSqlState(DataIntegrityViolationException violation) {
        // java.sql.SQLException (JDK standard, jamais le driver concret
        // PSQLException) suffit : le driver Postgresql (scope "runtime" dans
        // pom.xml, absent au compile) implemente cette interface, comme tout
        // driver JDBC. Evite une dependance de compilation inutile.
        Throwable cause = violation.getMostSpecificCause();
        if (cause instanceof java.sql.SQLException sqlException) {
            return sqlException.getSQLState();
        }
        return null;
    }
}
