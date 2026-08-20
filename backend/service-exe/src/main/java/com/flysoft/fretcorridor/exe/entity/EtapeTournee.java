package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// S11 (EF-MAT-05/06) : ordre planifié des étapes d'une Tournée consolidée
// (LTL), tel que publié par service-opt (TourneeConstitueeEvent). Distinct
// d'EtapeMission (chronologie RÉELLE, saisie par le chauffeur) — ceci est
// le PLAN, EtapeMission est l'EXÉCUTION.
//
// missionId référence Mission.id (même UUID qu'AffectationConfirmeeEvent,
// pas de FK inter-agrégat volontaire, même principe que Mission.demandeId).
// Contrainte unique (tourneeId, rang) : idempotence à la réingestion d'un
// même TourneeConstituee (rejeu Kafka), même patron que CapaciteEnAttente
// côté service-opt.
@Entity
@Table(name = "etapes_tournee", uniqueConstraints = @UniqueConstraint(columnNames = {"tournee_id", "rang"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapeTournee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tournee_id", nullable = false)
    private UUID tourneeId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(nullable = false)
    private int rang;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_etape", nullable = false)
    private TypeEtapeTournee typeEtape;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "point_latitude", nullable = false)
    private double pointLatitude;

    @Column(name = "point_longitude", nullable = false)
    private double pointLongitude;

    // Toujours null aujourd'hui côté OPT (RG-107 non branché) — cf.
    // TourneeConstitueeEvent, présent pour anticiper CDC §13 (Demande.fenetre).
    @Column(name = "fenetre_debut")
    private LocalDateTime fenetreDebut;

    @Column(name = "fenetre_fin")
    private LocalDateTime fenetreFin;

    public enum TypeEtapeTournee { ENLEVEMENT, LIVRAISON }
}
