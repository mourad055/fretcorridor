package com.fretcorridor.pay.infrastructure.messaging;

import com.fretcorridor.pay.domain.SequestreInvalideException;
import com.fretcorridor.pay.domain.SequestreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * RG-078, Item A de docs/DEPENDANCES_MOBILE_PHASE4.md : consomme
 * MissionLivree (service-exe) pour libérer le séquestre à la confirmation
 * de livraison — remplace l'appel REST synchrone POST
 * /confirmation-livraison, interdit entre porteurs par le Plan d'Exécution
 * §4.2/4.3. Même logique métier que le contrôleur, {@link SequestreService}
 * reste l'unique point d'entrée du domaine.
 *
 * Kafka livre au moins une fois : un rejeu du même eventId retombe sur un
 * séquestre déjà LIBERE, {@link SequestreService#liberer} refuse alors avec
 * {@link SequestreInvalideException} — attendu, pas une erreur, avalé ici
 * plutôt que de bloquer indéfiniment le groupe de consommateurs.
 */
@Component
public class MissionLivreeListener {

    private static final Logger log = LoggerFactory.getLogger(MissionLivreeListener.class);

    private final SequestreService sequestreService;

    public MissionLivreeListener(SequestreService sequestreService) {
        this.sequestreService = sequestreService;
    }

    @KafkaListener(topics = "mission-livree", containerFactory = "missionLivreeKafkaListenerContainerFactory")
    public void ingerer(MissionLivreeEvent event) {
        try {
            sequestreService.liberer(event.missionId(), event.tenantId(), event.transporteurId(),
                    event.preuveLivraisonReference());
            log.debug("Séquestre libéré depuis MissionLivree - mission={}, eventId={}", event.missionId(), event.eventId());
        } catch (SequestreInvalideException dejaTraiteOuHorsSequence) {
            log.warn("MissionLivree ignoré - mission={}, eventId={} - {}",
                    event.missionId(), event.eventId(), dejaTraiteOuHorsSequence.getMessage());
        }
    }
}
