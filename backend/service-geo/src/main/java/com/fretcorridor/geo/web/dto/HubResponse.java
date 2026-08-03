package com.fretcorridor.geo.web.dto;

import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.TypeHub;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de sortie : ne jamais exposer l'entite JPA (Hub) directement en reponse HTTP.
 * Ce decouplage protege contre deux choses :
 *   - une fuite accidentelle de champ interne le jour ou Hub gagne un attribut sensible
 *   - une rupture de contrat cote consommateur (Mobile/Web) si le modele JPA change en interne
 */
public record HubResponse(
        UUID id,
        String nom,
        String ville,
        TypeHub typeHub,
        double latitude,
        double longitude,
        // Expose pour verification manuelle (curl/tests) et pour que Mobile/Web
        // puissent eventuellement afficher/debugger le zonage sans requete separee.
        String h3Index,
        Instant dateCreation
) {
    // Factory de conversion Hub -> HubResponse : centralise la logique de mapping
    // en un seul endroit plutot que de la dupliquer a chaque usage dans le controller
    public static HubResponse from(Hub hub) {
        return new HubResponse(
                hub.getId(),
                hub.getNom(),
                hub.getVille(),
                hub.getTypeHub(),
                hub.getPosition().getY(), // JTS : Y = latitude
                hub.getPosition().getX(), // JTS : X = longitude (attention a l'ordre, source d'erreurs frequentes)
                hub.getH3Index(),
                hub.getDateCreation()
        );
    }
}
