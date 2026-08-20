package com.flysoft.fretcorridor.not.service;

import com.flysoft.fretcorridor.not.dto.NotificationDto;
import com.flysoft.fretcorridor.not.entity.FcmToken;
import com.flysoft.fretcorridor.not.entity.Notification;
import com.flysoft.fretcorridor.not.repository.FcmTokenRepository;
import com.flysoft.fretcorridor.not.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;

    // Appelée par les autres services (service-mkt, service-exe) quand un
    // événement mérite une notification. Pas encore de bus d'événements —
    // appel HTTP direct pour l'instant, migration Kafka possible plus tard.
    @Transactional
    public void creer(UUID destinataireActeurId, String titre, String corps,
                       Notification.TypeNotification type, UUID referenceId, String tenantId) {
        Notification notification = Notification.builder()
                .destinataireActeurId(destinataireActeurId)
                .titre(titre)
                .corps(corps)
                .type(type)
                .referenceId(referenceId)
                .tenantId(tenantId)
                .build();
        notificationRepository.save(notification);
        // TODO S9+ : déclencher le vrai push FCM ici une fois google-services.json disponible
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.NotificationResponse> getMesNotifications(UUID acteurId, String tenantId) {
        return notificationRepository.findByDestinataireActeurIdAndTenantIdOrderByDateCreationDesc(acteurId, tenantId)
                .stream().map(NotificationDto.NotificationResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public long getNombreNonLues(UUID acteurId, String tenantId) {
        return notificationRepository.countByDestinataireActeurIdAndTenantIdAndLueFalse(acteurId, tenantId);
    }

    // IDOR corrigé (audit CDC du 19 août, §3) : n'importe quel acteur
    // authentifié pouvait marquer/répondre à la notification de n'importe
    // qui en devinant un UUID. Une seule exception pour "introuvable" et
    // "pas la sienne" — ne pas distinguer les deux cas évite de confirmer
    // à un attaquant qu'un UUID existe (même principe que
    // missionAppartenantA côté service-exe).
    @Transactional
    public void marquerCommeLue(UUID notificationId, UUID acteurId) {
        Notification n = notificationAppartenantA(notificationId, acteurId);
        n.setLue(true);
        notificationRepository.save(n);
    }

    // S12 — réponse du chauffeur à une proposition de retour à vide.
    // Purement locale : aucun contrat pour relayer cette réponse au Moteur
    // à ce jour (voir Notification.reponseAcceptee).
    @Transactional
    public void repondre(UUID notificationId, UUID acteurId, boolean accepte) {
        Notification n = notificationAppartenantA(notificationId, acteurId);
        n.setReponseAcceptee(accepte);
        n.setDateReponse(java.time.LocalDateTime.now());
        n.setLue(true);
        notificationRepository.save(n);
    }

    private Notification notificationAppartenantA(UUID notificationId, UUID acteurId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationIntrouvableException());
        if (!n.getDestinataireActeurId().equals(acteurId)) {
            throw new NotificationIntrouvableException();
        }
        return n;
    }

    // Enregistrement du token FCM — préparé, pas encore exploité (voir README)
    @Transactional
    public void enregistrerToken(UUID acteurId, String token, String tenantId) {
        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                existant -> { existant.setActeurId(acteurId); fcmTokenRepository.save(existant); },
                () -> fcmTokenRepository.save(FcmToken.builder()
                        .acteurId(acteurId).token(token).tenantId(tenantId).build())
        );
    }
}
