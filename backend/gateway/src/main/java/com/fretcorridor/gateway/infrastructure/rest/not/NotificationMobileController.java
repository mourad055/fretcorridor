package com.fretcorridor.gateway.infrastructure.rest.not;

import com.fretcorridor.gateway.domain.not.NotificationMobilePort;
import com.fretcorridor.gateway.infrastructure.rest.not.dto.NotificationMobileResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/** S9 : centre de notifications de l'acteur mobile connecté. */
@RestController
@RequestMapping("/api/v1/notifications/mes")
public class NotificationMobileController {

    private final NotificationMobilePort notificationMobilePort;

    public NotificationMobileController(NotificationMobilePort notificationMobilePort) {
        this.notificationMobilePort = notificationMobilePort;
    }

    @GetMapping
    public Flux<NotificationMobileResponse> mesNotifications(@AuthenticationPrincipal AuthenticatedActor actor) {
        return notificationMobilePort.mesNotifications(actor.delegationToken()).map(NotificationMobileResponse::from);
    }

    @GetMapping("/non-lues")
    public Mono<Map<String, Integer>> nombreNonLues(@AuthenticationPrincipal AuthenticatedActor actor) {
        return notificationMobilePort.nombreNonLues(actor.delegationToken()).map(n -> Map.of("nombre", n));
    }

    @PatchMapping("/{id}/lue")
    public Mono<ResponseEntity<Void>> marquerLue(@PathVariable String id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return notificationMobilePort.marquerLue(actor.delegationToken(), id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
