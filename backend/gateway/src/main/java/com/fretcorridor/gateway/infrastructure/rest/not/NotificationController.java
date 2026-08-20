package com.fretcorridor.gateway.infrastructure.rest.not;

import com.fretcorridor.gateway.domain.not.NotificationPort;
import com.fretcorridor.gateway.infrastructure.rest.not.dto.NotificationResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Centre de notifications côté web (Sprint 9). Un Bureau ne voit que les
 * notifications de son propre tenant (ENF-MUL-01).
 */
@RestController
public class NotificationController {

    private final NotificationPort notificationPort;

    public NotificationController(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @GetMapping("/api/v1/bureau/notifications")
    public Flux<NotificationResponse> notificationsBureau(@AuthenticationPrincipal AuthenticatedActor actor) {
        return notificationPort.listerNotificationsParTenant(actor.tenantId()).map(NotificationResponse::from);
    }
}
