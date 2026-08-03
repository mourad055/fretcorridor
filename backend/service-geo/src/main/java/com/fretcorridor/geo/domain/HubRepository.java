package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Acces donnees pour Hub. JpaRepository fournit deja findAll/findById/save/delete :
 * pas besoin de les reecrire tant qu'aucune requete geospatiale custom (ex. "hubs dans un rayon
 * de X km", futur besoin pour le filtrage L0 d'OPT) n'est necessaire.
 */
public interface HubRepository extends JpaRepository<Hub, UUID> {
    // A completer plus tard : requetes spatiales (ex. via @Query + fonctions PostGIS ST_DWithin)
    // quand OPT aura besoin du filtrage geospatial reel, cf Sprint 5.
}
