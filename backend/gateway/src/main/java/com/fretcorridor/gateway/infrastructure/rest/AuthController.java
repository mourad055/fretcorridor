package com.fretcorridor.gateway.infrastructure.rest;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationPort;
import com.fretcorridor.gateway.infrastructure.rest.dto.LoginRequest;
import com.fretcorridor.gateway.infrastructure.rest.dto.LoginResponse;
import com.fretcorridor.gateway.infrastructure.rest.dto.RegisterRequest;
import com.fretcorridor.gateway.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * FE-WEB-01 : écran de connexion unique (téléphone + code), aucune indication de rôle
 * avant authentification réussie — c'est cette contrainte qui interdit tout endpoint
 * "liste des rôles disponibles" avant login.
 */
@RestController
public class AuthController {

    private final AuthenticationPort authenticationPort;
    private final JwtService jwtService;

    public AuthController(AuthenticationPort authenticationPort, JwtService jwtService) {
        this.authenticationPort = authenticationPort;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/v1/auth/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authenticationPort.authenticate(request.phone(), request.code())
                .map(this::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/api/v1/auth/register")
    public Mono<ResponseEntity<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authenticationPort.register(request.phone(), request.code(), request.type(),
                        request.nom(), request.prenom(), request.raisonSociale())
                .map(this::toResponse)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    private LoginResponse toResponse(Actor actor) {
        String token = jwtService.issue(actor);
        return new LoginResponse(token, actor.role().name(), actor.tenantId());
    }
}
