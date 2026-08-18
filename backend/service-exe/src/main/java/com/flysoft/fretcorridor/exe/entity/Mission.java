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
    private UUID axeId;
    private String origineNom;
    private String destinationNom;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutMission statut = StatutMission.EN_ATTENTE;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum StatutMission { EN_ATTENTE, PRISE_EN_CHARGE, EN_TRANSIT, LIVREE, ANNULEE }
}
