package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.CapaciteDeclarationPort;
import com.fretcorridor.gateway.domain.cap.DeclarationCapacite;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.CapaciteDeclareeResponse;
import com.fretcorridor.gateway.infrastructure.rest.cap.dto.DeclarerCapaciteRequest;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** S4 (EF-CAP-03/07) : déclaration de capacité par le chauffeur/transporteur. */
@RestController
public class CapaciteDeclarationController {

    private final CapaciteDeclarationPort capaciteDeclarationPort;

    public CapaciteDeclarationController(CapaciteDeclarationPort capaciteDeclarationPort) {
        this.capaciteDeclarationPort = capaciteDeclarationPort;
    }

    @PostMapping("/api/v1/capacites")
    public Mono<ResponseEntity<CapaciteDeclareeResponse>> declarer(@Valid @RequestBody DeclarerCapaciteRequest request,
                                                                     @AuthenticationPrincipal AuthenticatedActor actor) {
        var requete = new DeclarationCapacite(
                request.vehiculeId(), request.axeId(), request.modeDeclaration(),
                request.poidsKg(), request.volumeM3(), request.longueurPlancherM(),
                request.origineLatitude(), request.origineLongitude(), request.typeVehicule(),
                request.profilHauteurMetres(), request.profilLargeurMetres(), request.profilLongueurMetres(),
                request.profilPoidsMaxTonnes(), request.profilChargeMaxParEssieuTonnes(),
                request.profilNombreEssieux(), request.profilMatieresDangereuses(), request.dateDepart());

        return capaciteDeclarationPort.declarer(requete, actor.delegationToken())
                .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(CapaciteDeclareeResponse.from(c)));
    }
}
