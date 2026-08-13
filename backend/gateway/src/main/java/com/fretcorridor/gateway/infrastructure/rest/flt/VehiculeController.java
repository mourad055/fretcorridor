package com.fretcorridor.gateway.infrastructure.rest.flt;

import com.fretcorridor.gateway.domain.flt.DeclarationVehicule;
import com.fretcorridor.gateway.domain.flt.VehiculePort;
import com.fretcorridor.gateway.infrastructure.rest.flt.dto.DeclarerVehiculeRequest;
import com.fretcorridor.gateway.infrastructure.rest.flt.dto.VehiculeResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** S10 : console de flotte simplifiée (mode transporteur étendu). */
@RestController
@RequestMapping("/api/v1/vehicules")
public class VehiculeController {

    private final VehiculePort vehiculePort;

    public VehiculeController(VehiculePort vehiculePort) {
        this.vehiculePort = vehiculePort;
    }

    @PostMapping
    public Mono<ResponseEntity<VehiculeResponse>> declarer(@Valid @RequestBody DeclarerVehiculeRequest request,
                                                             @AuthenticationPrincipal AuthenticatedActor actor) {
        var declaration = new DeclarationVehicule(request.typeVehicule(), request.immatriculation(),
                request.profilHauteurMetres(), request.profilLargeurMetres(), request.profilLongueurMetres(),
                request.profilPoidsMaxTonnes(), request.profilChargeMaxParEssieuTonnes(), request.profilNombreEssieux(),
                request.profilMatieresDangereuses());
        return vehiculePort.declarer(actor.delegationToken(), declaration)
                .map(v -> ResponseEntity.status(HttpStatus.CREATED).body(VehiculeResponse.from(v)));
    }

    @GetMapping("/mes")
    public Flux<VehiculeResponse> mesVehicules(@AuthenticationPrincipal AuthenticatedActor actor) {
        return vehiculePort.mesVehicules(actor.delegationToken()).map(VehiculeResponse::from);
    }
}
