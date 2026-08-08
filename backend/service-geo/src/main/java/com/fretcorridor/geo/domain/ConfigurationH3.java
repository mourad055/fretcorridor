package com.fretcorridor.geo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Parametre de configuration versionne pour le zonage H3 (ex. resolution_defaut).
 * Mappe la table geo.configuration_h3 (cf migration V3).
 *
 * Existe precisement pour eviter l'anti-patron explicite du CDC S12.4 :
 * aucune resolution H3, aucun seuil ne doit etre code en dur dans le service -
 * tout passe par cette table, modifiable sans redeploiement.
 */
@Entity
@Table(name = "configuration_h3", schema = "geo")
public class ConfigurationH3 {

    // Cle metier utilisee comme identifiant naturel (ex. "resolution_defaut"),
    // plutot qu'un UUID technique : cette table est une liste de parametres nommes,
    // pas une collection d'entites au sens strict.
    @Id
    @Column(name = "cle", length = 50)
    private String cle;

    @Column(name = "valeur", nullable = false, length = 50)
    private String valeur;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "date_modification", nullable = false)
    private Instant dateModification;

    protected ConfigurationH3() {
        // Requis par JPA.
    }

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDateModification() {
        return dateModification;
    }
}
