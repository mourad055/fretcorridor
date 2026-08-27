package com.fretcorridor.gateway.infrastructure.not;

import com.fretcorridor.gateway.domain.not.CanalNotification;
import com.fretcorridor.gateway.domain.not.Notification;
import com.fretcorridor.gateway.domain.not.NotificationPort;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Double test-only — remplacé en prod par {@link RealNotAdapter}. */
public class MockNotAdapter implements NotificationPort {

    private final List<Notification> notifications = List.of(
            new Notification("not-1", "tenant-bgft-douala", CanalNotification.EMAIL,
                    "bureau.douala@bgft.example", "Nouvelle mission appariée",
                    "La mission Douala → Yaoundé a été appariée à un transporteur.",
                    Instant.now().minus(2, ChronoUnit.HOURS)),
            new Notification("not-2", "tenant-bgft-douala", CanalNotification.EMAIL,
                    "bureau.douala@bgft.example", "Écart de réconciliation détecté",
                    "Une alerte de réconciliation a été levée sur la mission mission-a.",
                    Instant.now().minus(1, ChronoUnit.DAYS)),
            new Notification("not-3", "tenant-bnft-ndjamena", CanalNotification.EMAIL,
                    "bureau.tchad@bnft.example", "Dossier KYC validé",
                    "Le dossier KYC d'un transporteur de votre territoire a été validé.",
                    Instant.now().minus(3, ChronoUnit.HOURS)),
            new Notification("not-4", "tenant-bnft-ndjamena", CanalNotification.EMAIL,
                    "bureau.tchad@bnft.example", "Nouvelle mission appariée",
                    "La mission N'Djamena → Sarh a été appariée à un transporteur.",
                    Instant.now().minus(45, ChronoUnit.MINUTES))
    );

    @Override
    public Flux<Notification> listerNotificationsParTenant(String tenantId, String delegationToken) {
        return Flux.fromIterable(notifications).filter(n -> n.tenantId().equals(tenantId));
    }
}
