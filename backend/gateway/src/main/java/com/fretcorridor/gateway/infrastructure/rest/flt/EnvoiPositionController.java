package com.fretcorridor.gateway.infrastructure.rest.flt;

import com.fretcorridor.gateway.domain.flt.PositionEnvoi;
import com.fretcorridor.gateway.domain.flt.PositionPort;
import com.fretcorridor.gateway.infrastructure.rest.flt.dto.EnvoyerPositionRequest;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** S6 (EF-TRK-01) : envoi de positions GPS par le chauffeur en mission. */
@RestController
public class EnvoiPositionController {

    private final PositionPort positionPort;

    public EnvoiPositionController(PositionPort positionPort) {
        this.positionPort = positionPort;
    }

    @PostMapping("/api/v1/positions")
    public Mono<ResponseEntity<Void>> envoyer(@Valid @RequestBody EnvoyerPositionRequest request,
                                               @AuthenticationPrincipal AuthenticatedActor actor) {
        var position = new PositionEnvoi(request.missionId(), request.latitude(), request.longitude(),
                request.horodatage(), request.eventId(), request.sourceCapture(), request.precisionMetres());
        return positionPort.envoyer(actor.delegationToken(), position)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).<Void>build()));
    }
}
