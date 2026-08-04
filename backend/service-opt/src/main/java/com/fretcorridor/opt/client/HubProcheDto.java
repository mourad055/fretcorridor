package com.fretcorridor.opt.client;

import java.util.UUID;

/**
 * Representation cote OPT d'un hub retourne par service-geo (/api/geo/zonage/hubs-proches).
 *
 * Volontairement distinct du HubResponse de service-geo : chaque service possede
 * son propre contrat d'entree/sortie (cf shared-contracts/ pour la version figee
 * en OpenAPI a terme) - ca evite qu'un changement interne a service-geo casse
 * silencieusement service-opt sans passer par une revue de contrat explicite.
 *
 * Ne reprend que les champs utiles au filtrage L0 - pas dateCreation, par exemple.
 */
public record HubProcheDto(
        UUID id,
        String nom,
        String ville,
        double latitude,
        double longitude,
        String h3Index
) {
}
