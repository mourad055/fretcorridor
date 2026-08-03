package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acces donnees pour Hub. JpaRepository fournit deja findAll/findById/save/delete :
 * pas besoin de les reecrire tant qu'aucune requete geospatiale custom (ex. "hubs dans un rayon
 * de X km", futur besoin pour le filtrage L0 d'OPT) n'est necessaire.
 */
public interface HubRepository extends JpaRepository<Hub, UUID> {

    // Coeur du filtre L0 d'OPT : retrouver tous les hubs dont l'index H3 correspond
    // a la cellule cherchee ou a l'une de ses voisines (k-ring calcule cote service,
    // pas ici - ce repository ne fait que filtrer sur une liste d'index deja resolue).
    List<Hub> findByH3IndexIn(List<String> indexH3);
}
