package com.fretcorridor.geo.domain;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * Noeud du reseau geospatial : ville, plateforme ou point de consolidation.
 * cf CDC v4 §13 - Entite Hub. Relation n-n avec Axe.
 */
@Entity
@Table(name = "hub", schema = "geo")
public class Hub {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 150)
    private String ville;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_hub", nullable = false, length = 30)
    private TypeHub typeHub;

    // SRID 4326 (WGS84) - coherent avec les coordonnees GPS brutes captures par FLT
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point position;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected Hub() {
        // requis par JPA
    }

    public Hub(String nom, String ville, TypeHub typeHub, Point position) {
        this.nom = nom;
        this.ville = ville;
        this.typeHub = typeHub;
        this.position = position;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }
    public TypeHub getTypeHub() { return typeHub; }
    public Point getPosition() { return position; }
    public Instant getDateCreation() { return dateCreation; }
}
