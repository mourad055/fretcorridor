package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// UC-EXE-01 : suivi d'exécution d'une mission — vue lecture seule pour le
// client (EF-EXE-02 complet avec enlèvements/livraisons multiples est un
// chantier plus large, reporté à la construction de l'app Chauffeur).
@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID demandeId; // référence service-mkt — pas de FK inter-service

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutMission statut = StatutMission.EN_ATTENTE;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum StatutMission { EN_ATTENTE, PRISE_EN_CHARGE, EN_TRANSIT, LIVREE, ANNULEE }
}
