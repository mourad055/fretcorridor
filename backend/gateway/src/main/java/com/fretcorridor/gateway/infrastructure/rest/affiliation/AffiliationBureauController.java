package com.fretcorridor.gateway.infrastructure.rest.affiliation;

import com.fretcorridor.gateway.domain.affiliation.AffiliationPort;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * S18 : invitation d'un transporteur/chauffeur par le second bureau — règle
 * produit choisie par l'utilisatrice, l'invitation EST la validation (aucun
 * flux d'acceptation côté transporteur). Chemin "/api/v1/bureau/**" déjà
 * réservé au rôle BUREAU par SecurityConfig (RG-002, pathMatchers) — même
 * mécanisme que les autres endpoints Bureau de ce dépôt, pas de vérification
 * de rôle supplémentaire nécessaire ici.
 */
@RestController
@RequestMapping("/api/v1/bureau/affiliations")
public class AffiliationBureauController {

    private final AffiliationPort affiliationPort;

    public AffiliationBureauController(AffiliationPort affiliationPort) {
        this.affiliationPort = affiliationPort;
    }

    public record InviterRequest(@NotBlank String telephone) {
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> inviter(@Valid @RequestBody InviterRequest request,
                                               @AuthenticationPrincipal AuthenticatedActor actor) {
        return affiliationPort.inviter(actor.delegationToken(), request.telephone())
                .thenReturn(ResponseEntity.status(201).<Void>build());
    }
}
