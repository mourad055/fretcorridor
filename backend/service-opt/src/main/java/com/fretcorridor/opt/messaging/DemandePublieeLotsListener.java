package com.fretcorridor.opt.messaging;

import com.fretcorridor.opt.oracle.LotDemande;
import com.fretcorridor.opt.oracle.LotDemandeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemandePublieeLotsListener {

    private static final Logger log = LoggerFactory.getLogger(DemandePublieeLotsListener.class);

    private final LotDemandeRepository lotDemandeRepository;

    public DemandePublieeLotsListener(LotDemandeRepository lotDemandeRepository) {
        this.lotDemandeRepository = lotDemandeRepository;
    }

    @KafkaListener(topics = "demande-publiee-lots", containerFactory = "demandePublieeLotsKafkaListenerContainerFactory")
    public void ingerer(DemandePublieeLotsEvent event) {
        for (LotPayload lot : event.lots()) {
            try {
                lotDemandeRepository.save(new LotDemande(
                        event.demandeId(), lot.lotId(), event.eventId(), lot.typeCatalogue(), lot.quantite(),
                        BigDecimal.valueOf(lot.poidsKg()), lot.longueurM(), lot.largeurM(), lot.hauteurM(),
                        lot.gerbable(), lot.fragile(), lot.classeDanger()));
            } catch (DataIntegrityViolationException violation) {
                // FIX audit 21/08 : depuis V17 (UNIQUE(event_id, lot_id)), le
                // catch générique confondait deux causes très différentes.
                // SQLState 23505 (unique_violation) = doublon Kafka réel
                // (même event + même lot rejoué), bénin, ignoré comme prévu.
                // Tout autre SQLState (ex. 23502 NOT NULL sur poids_kg si le
                // producteur envoie un champ requis manquant) est une VRAIE
                // perte de donnée - ne doit jamais passer pour de
                // l'idempotence. Même pattern que CapaciteDeclareeListener
                // (correctif 64a91a5 du 2026-08-17), non reporté ici jusqu'à
                // présent.
                String sqlState = extraireSqlState(violation);
                if ("23505".equals(sqlState)) {
                    log.info("Lot deja ingere, doublon ignore - eventId={}, lotId={}", event.eventId(), lot.lotId());
                } else {
                    log.error("Echec ingestion lot (contrainte violee, SQLState={}) - lot NON enregistre, "
                            + "demande={}, lotId={}, eventId={} - a investiguer cote service-mkt "
                            + "(champ requis manquant ?)",
                            sqlState, event.demandeId(), lot.lotId(), event.eventId(), violation);
                }
            }
        }
        log.info("DemandePublieeLots recue - demande={}, {} lot(s)", event.demandeId(), event.lots().size());
    }

    private static String extraireSqlState(DataIntegrityViolationException violation) {
        // java.sql.SQLException (JDK standard, jamais le driver concret
        // PSQLException) suffit : le driver Postgresql implemente cette
        // interface. Meme justification que CapaciteDeclareeListener -
        // evite une dependance de compilation au driver.
        Throwable cause = violation.getMostSpecificCause();
        if (cause instanceof java.sql.SQLException sqlException) {
            return sqlException.getSQLState();
        }
        return null;
    }
}
