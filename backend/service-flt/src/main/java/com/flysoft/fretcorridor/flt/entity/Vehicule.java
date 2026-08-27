package com.flysoft.fretcorridor.flt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// S10 : console de flotte simplifiée (mode transporteur étendu). Registre
// minimal — ferme le TODO laissé au S4 (déclaration de capacité) où
// vehiculeId était géré localement sur l'appareil faute de registre serveur.
@Entity
@Table(name = "vehicules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID proprietaireActeurId; // référence service-ida — pas de FK inter-service

    @Column(nullable = false)
    private String typeVehicule;

    // RG-088 (audit CDC du 19 août, bloquant corrigé) : une immatriculation
    // identifie un véhicule physique unique, indépendamment du tenant qui le
    // déclare — deux tenants ne doivent jamais pouvoir déclarer la même
    // plaque. NULL reste autorisé plusieurs fois (contrainte SQL standard) :
    // un véhicule pas encore immatriculé (neuf, en cours de démarches).
    @Column(unique = true)
    private String immatriculation;

    private Double profilHauteurMetres;
    private Double profilLargeurMetres;
    private Double profilLongueurMetres;
    private Double profilPoidsMaxTonnes;
    private Double profilChargeMaxParEssieuTonnes;
    private Integer profilNombreEssieux;

    @Builder.Default
    private boolean profilMatieresDangereuses = false;

    // Photos de la carte grise (retour utilisatrice 24/08) -- clés d'objet
    // MinIO (VehiculePhotoStorageService), jamais l'URL/le contenu stockés
    // ici directement (même principe que les preuves de mission côté
    // service-exe).
    private String photoCarteGriseRectoKey;
    private String photoCarteGriseVersoKey;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
