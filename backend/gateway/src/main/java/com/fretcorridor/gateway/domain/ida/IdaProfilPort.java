package com.fretcorridor.gateway.domain.ida;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Complétion du profil KYC niveau 1 par l'acteur lui-même (RG-011, Sprint 2).
 * Toute implémentation appelle service-ida pour le compte de l'acteur connecté
 * avec son delegationToken (cf. AuthenticatedActor) — jamais le JWT du gateway.
 */
public interface IdaProfilPort {

    Mono<Profil> profil(String delegationToken);

    Mono<Profil> completerParticulier(String delegationToken, String nom, String prenom);

    Mono<Profil> completerEntreprise(String delegationToken, String raisonSociale, String numeroRegistreCommerce);

    Mono<Profil> deposerPiece(String delegationToken, String typeDocument, FilePart fichier);
}
