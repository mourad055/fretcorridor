package com.fretcorridor.gateway.infrastructure.rest.ida;

import com.fretcorridor.gateway.domain.ida.IdaProfilPort;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.CompleterEntrepriseRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.CompleterParticulierRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.ModifierTelephoneRequest;
import com.fretcorridor.gateway.infrastructure.rest.ida.dto.ProfilResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * RG-011 (Sprint 2) : complétion du profil KYC niveau 1 par l'acteur
 * lui-même — commun aux deux apps mobiles (Chauffeur/Transporteur et
 * Client), pas de restriction de rôle au-delà de l'authentification.
 */
@RestController
@RequestMapping("/api/v1/kyc")
public class ProfilController {

    private final IdaProfilPort idaProfilPort;

    public ProfilController(IdaProfilPort idaProfilPort) {
        this.idaProfilPort = idaProfilPort;
    }

    @GetMapping("/profil")
    public Mono<ProfilResponse> profil(@AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.profil(actor.delegationToken()).map(ProfilResponse::from);
    }

    @PutMapping("/profil/particulier")
    public Mono<ProfilResponse> completerParticulier(@Valid @RequestBody CompleterParticulierRequest request,
                                                       @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.completerParticulier(actor.delegationToken(), request.nom(), request.prenom())
                .map(ProfilResponse::from);
    }

    @PutMapping("/profil/entreprise")
    public Mono<ProfilResponse> completerEntreprise(@Valid @RequestBody CompleterEntrepriseRequest request,
                                                      @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.completerEntreprise(actor.delegationToken(), request.raisonSociale(), request.numeroRegistreCommerce())
                .map(ProfilResponse::from);
    }

    @PutMapping("/profil/telephone")
    public Mono<java.util.Map<String, String>> modifierTelephone(@Valid @RequestBody ModifierTelephoneRequest request,
                                                                    @AuthenticationPrincipal AuthenticatedActor actor) {
        return idaProfilPort.modifierTelephone(actor.delegationToken(), request.ancienTelephone(), request.nouveauTelephone())
                .map(telephone -> java.util.Map.of("telephone", telephone));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ProfilResponse> deposerPiece(ServerWebExchange exchange,
                                              @AuthenticationPrincipal AuthenticatedActor actor) {
        return exchange.getMultipartData().flatMap(parts -> {
            var fichierPart = parts.getFirst("fichier");
            var typeDocumentPart = parts.getFirst("typeDocument");
            if (!(fichierPart instanceof FilePart fichier) || !(typeDocumentPart instanceof FormFieldPart typeDocument)) {
                return Mono.error(new PieceManquanteException());
            }
            return idaProfilPort.deposerPiece(actor.delegationToken(), typeDocument.value(), fichier);
        }).map(ProfilResponse::from);
    }

    public static class PieceManquanteException extends RuntimeException {
        PieceManquanteException() {
            super("Les champs \"fichier\" et \"typeDocument\" sont obligatoires");
        }
    }
}
