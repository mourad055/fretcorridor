package com.flysoft.fretcorridor.not.messaging;

import com.flysoft.fretcorridor.not.client.ServiceCapClient;
import com.flysoft.fretcorridor.not.entity.Notification;
import com.flysoft.fretcorridor.not.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consomme proposition-retour-a-vide (service-opt, Moteur) — S12, écran
 * "notification de mission retour, acceptation/refus" côté app
 * Chauffeur/Transporteur.
 *
 * tourneeId/affectationId mutuellement exclusifs (contrat) : testé
 * explicitement, jamais supposé qu'un seul des deux est toujours présent.
 * referenceId de la notification = celui des deux qui est renseigné.
 */
@Component
public class PropositionRetourAVideListener {

    private static final Logger log = LoggerFactory.getLogger(PropositionRetourAVideListener.class);

    private final ServiceCapClient serviceCapClient;
    private final NotificationService notificationService;
    // Même hypothèse mono-tenant que les autres consommateurs d'événements
    // Moteur (AffectationConfirmeeListener, service-exe, ADR 0011) — corrigé
    // le 18 août : hardcodait auparavant un UUID différent de
    // fretcorridor.exe.tenant-id-phase1 (tenant-bgft-douala), ce qui aurait
    // rendu les notifications PROPOSITION_RETOUR invisibles côté app (filtre
    // par tenantId du JWT dans NotificationController). Même propriété que
    // service-exe pour rester cohérent avec le reste de la chaîne S7/S12.
    private final String tenantIdPhase1;

    public PropositionRetourAVideListener(ServiceCapClient serviceCapClient, NotificationService notificationService,
                                           @Value("${fretcorridor.not.tenant-id-phase1}") String tenantIdPhase1) {
        this.serviceCapClient = serviceCapClient;
        this.notificationService = notificationService;
        this.tenantIdPhase1 = tenantIdPhase1;
    }

    @KafkaListener(topics = "proposition-retour-a-vide",
            containerFactory = "propositionRetourAVideKafkaListenerContainerFactory")
    public void ingerer(PropositionRetourAVideEvent event) {
        UUID referenceId = event.tourneeId() != null ? event.tourneeId() : event.affectationId();
        if (referenceId == null) {
            log.warn("PropositionRetourAVide sans tourneeId ni affectationId - eventId={}, evenement ignore",
                    event.eventId());
            return;
        }

        serviceCapClient.resoudreTransporteur(event.capaciteId()).ifPresentOrElse(
                transporteurId -> notificationService.creer(
                        transporteurId,
                        "Mission de retour disponible",
                        "Un retour à vide est proposé après votre livraison. Consultez les détails pour accepter ou refuser.",
                        Notification.TypeNotification.PROPOSITION_RETOUR,
                        referenceId,
                        tenantIdPhase1),
                () -> log.warn("PropositionRetourAVide non notifiee (transporteur non resolu) - "
                        + "capacite={}, eventId={}", event.capaciteId(), event.eventId())
        );
    }
}
