package com.fretcorridor.gateway.infrastructure.rest.ida;

import com.fretcorridor.gateway.domain.ida.IdaProfilPort;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.CompleterEntrepriseRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.CompleterParticulierRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.ProfilResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * RG-011 (Sprint 2) : complétion du profil KYC niveau 1 par l'acteur
 * lui-même — commun aux deux apps mobiles (Chauffeur/Transporteur et
 * Client), pas de restriction de rôle au-delà de l'authentification.
 */
@RestController
@RequestMapping("/api/v1/kyc/profil")
public class ProfilController {

    private final IdaProfilPort idaProfilPort;

    public ProfilController(IdaProfilPort idaProfilPort) {
        this.idaProfilPort = idaProfilPort;
    }

    @GetMapping
    public Mono<ProfilResponse> profil(@AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.profil(actor.delegationToken()).map(ProfilResponse::from);
    }

    @PutMapping("/particulier")
    public Mono<ProfilResponse> completerParticulier(@Valid @RequestBody CompleterParticulierRequest request,
                                                       @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.completerParticulier(actor.delegationToken(), request.nom(), request.prenom())
                .map(ProfilResponse::from);
    }

    @PutMapping("/entreprise")
    public Mono<ProfilResponse> completerEntreprise(@Valid @RequestBody CompleterEntrepriseRequest request,
                                                      @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.completerEntreprise(actor.delegationToken(), request.raisonSociale(), request.numeroRegistreCommerce())
                .map(ProfilResponse::from);
    }
}
