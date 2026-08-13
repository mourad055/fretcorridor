package com.fretcorridor.gateway.infrastructure.rest.agent;

import com.fretcorridor.gateway.domain.agent.AgentEnrolementPort;
import com.fretcorridor.gateway.infrastructure.rest.agent.dto.ActiverEnrolementRequest;
import com.fretcorridor.gateway.infrastructure.rest.agent.dto.EnrolementResponse;
import com.fretcorridor.gateway.infrastructure.rest.agent.dto.InitierEnrolementRequest;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** UC-IDA-03 (Sprint 2 bis) : enrôlement assisté par agent de terrain (EF-IDA-06). */
@RestController
@RequestMapping("/api/v1/agent/enrolements")
public class AgentEnrolementController {

    private final AgentEnrolementPort agentEnrolementPort;

    public AgentEnrolementController(AgentEnrolementPort agentEnrolementPort) {
        this.agentEnrolementPort = agentEnrolementPort;
    }

    @PostMapping
    public Mono<ResponseEntity<EnrolementResponse>> initier(@Valid @RequestBody InitierEnrolementRequest request,
                                                              @AuthenticationPrincipal AuthenticatedActor actor) {
        return agentEnrolementPort.initier(actor.delegationToken(), request.telephone(), request.typeActeur(),
                        request.latitude(), request.longitude(), request.idempotencyKey())
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(EnrolementResponse.from(e)));
    }

    @PostMapping("/{id}/activation")
    public Mono<EnrolementResponse> activer(@PathVariable String id,
                                             @Valid @RequestBody ActiverEnrolementRequest request,
                                             @AuthenticationPrincipal AuthenticatedActor actor) {
        return agentEnrolementPort.activer(actor.delegationToken(), id, request.otp(), request.codePin())
                .map(EnrolementResponse::from);
    }
}
