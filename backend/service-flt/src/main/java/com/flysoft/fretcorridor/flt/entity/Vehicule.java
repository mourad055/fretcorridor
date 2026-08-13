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

    private String immatriculation;

    private Double profilHauteurMetres;
    private Double profilLargeurMetres;
    private Double profilLongueurMetres;
    private Double profilPoidsMaxTonnes;
    private Double profilChargeMaxParEssieuTonnes;
    private Integer profilNombreEssieux;

    @Builder.Default
    private boolean profilMatieresDangereuses = false;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
