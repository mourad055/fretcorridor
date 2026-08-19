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
            } catch (DataIntegrityViolationException doublon) {
                log.info("Lot deja ingere (idempotence) - eventId={}, lotId={}", event.eventId(), lot.lotId());
            }
        }
        log.info("DemandePublieeLots recue - demande={}, {} lot(s)", event.demandeId(), event.lots().size());
    }
}
