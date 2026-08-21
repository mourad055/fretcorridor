package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// UC-EXE-01 : suivi d'exécution d'une mission — vue lecture seule pour le
// client (EF-EXE-02 complet avec enlèvements/livraisons multiples est un
// chantier plus large, reporté à la construction de l'app Chauffeur).
//
// {@code id} n'est PAS auto-généré : c'est le missionId minté par service-opt
// et porté par l'événement AffectationConfirmee (cf. AffectationConfirmeeListener,
// S7) — cette identité doit être commune à service-exe, service-flt
// (Position.missionId) et service-opt, jamais régénérée localement.
@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID demandeId; // référence service-mkt — pas de FK inter-service

    // S7 : renseigné par AffectationConfirmeeListener (référence service-ida,
    // pas de FK inter-service) — condition pour que le chauffeur voie "ses"
    // missions (EF-EXE-02, jusqu'ici hors périmètre, cf. commentaire ci-dessus).
    private UUID transporteurId;

    // Renseigné par AffectationConfirmeeListener quand service-opt a résolu
    // le véhicule affecté (référence service-flt, pas de FK inter-service) -
    // permet à service-flt de rattacher une position GPS au bon véhicule
    // avant publication de PositionBrute (cf audit §7.1, canal Kafka mort).
    private UUID vehiculeId;

    private UUID axeId;
    private String origineNom;
    private String destinationNom;

    // Infos marchandise (audit de suivi Mobile) : renseigné par
    // AffectationConfirmeeListener — l'app Chauffeur n'avait jusqu'ici
    // aucun moyen de savoir ce qu'elle transporte pour une mission donnée.
    private String typeEmballageNom;
    private Integer quantite;
    private java.math.BigDecimal poidsTaxableKg;

    // S11 (EF-MAT-05/06) : renseigné par TourneeConstitueeListener quand
    // cette Mission fait partie d'une Tournée consolidée (LTL) — null pour
    // une affectation FTL simple, jamais regroupée par service-opt.
    private UUID tourneeId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutMission statut = StatutMission.EN_ATTENTE;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum StatutMission { EN_ATTENTE, PRISE_EN_CHARGE, EN_TRANSIT, LIVREE, ANNULEE }
}
