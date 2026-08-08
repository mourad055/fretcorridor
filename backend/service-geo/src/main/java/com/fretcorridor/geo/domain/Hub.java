package com.fretcorridor.geo.domain;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * Noeud du reseau geospatial : ville, plateforme ou point de consolidation.
 * cf CDC v4 S13 - Entite Hub. Relation n-n avec Axe (a implementer dans un increment suivant).
 *
 * Cette entite appartient au schema "geo" (isolation par service, cf Plan d'execution S4.1) :
 * aucun autre microservice n'a d'acces direct a cette table, tout passe par l'API REST
 * (synchrone pour OPT/TRK, meme porteur) ou par evenement Kafka (autre porteur).
 */
@Entity
@Table(name = "hub", schema = "geo")
public class Hub {

    // UUID genere par la base (gen_random_uuid(), cf migration Flyway V1) plutot que par l'appli :
    // evite toute collision entre plusieurs instances du service scale horizontalement
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 150)
    private String ville;

    // Stocke en base comme VARCHAR (cf contrainte CHECK dans la migration SQL),
    // mappe ici en enum cote Java pour la securite de type
    @Enumerated(EnumType.STRING)
    @Column(name = "type_hub", nullable = false, length = 30)
    private TypeHub typeHub;

    // SRID 4326 (WGS84) : coherent avec les coordonnees GPS brutes capturees par le module FLT
    // (mobile), indispensable pour que TRK puisse comparer position vehicule et position hub
    // sans conversion de referentiel
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point position;

    // Index H3 de la cellule hexagonale contenant ce hub (cf migration V3).
    // Calcule une seule fois a la creation par ZonageH3Service (pas ici : l'entite
    // n'a pas acces a la resolution configurable, qui vit en base) - jamais recalcule
    // ensuite, un hub ne change pas de position une fois cree.
    @Column(name = "h3_index", length = 20)
    private String h3Index;

    // Renseigne automatiquement a la creation (cf @PrePersist ci-dessous), jamais par le client :
    // evite toute incoherence si un appelant envoie une date fantaisiste
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    // Constructeur vide requis par JPA (proxy, hydratation reflexive) - jamais utilise directement
    protected Hub() {
    }

    // Constructeur metier historique (sans h3Index) - conserve pour compatibilite,
    // mais deprecie : passer desormais par le constructeur avec h3Index pour que
    // le zonage soit toujours renseigne des la creation.
    @Deprecated
    public Hub(String nom, String ville, TypeHub typeHub, Point position) {
        this(nom, ville, typeHub, position, null);
    }

    // Constructeur metier complet : c'est celui-ci que le controller utilise desormais,
    // avec l'index H3 deja calcule en amont par ZonageH3Service.
    public Hub(String nom, String ville, TypeHub typeHub, Point position, String h3Index) {
        this.nom = nom;
        this.ville = ville;
        this.typeHub = typeHub;
        this.position = position;
        this.h3Index = h3Index;
    }

    // Hook JPA declenche juste avant l'INSERT : garantit que dateCreation est toujours
    // le timestamp serveur au moment de la persistance, jamais laisse a null ni falsifiable
    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    // Getters uniquement : l'entite est immuable une fois creee (pas de setters).
    // Toute evolution future (ex. desactivation d'un hub) devra passer par une methode
    // metier explicite plutot qu'un setter generique.
    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }
    public TypeHub getTypeHub() { return typeHub; }
    public Point getPosition() { return position; }
    public String getH3Index() { return h3Index; }
    public Instant getDateCreation() { return dateCreation; }
}
