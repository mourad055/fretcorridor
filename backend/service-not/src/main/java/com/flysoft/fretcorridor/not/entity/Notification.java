package com.flysoft.fretcorridor.not.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// EF-NOT (implicite) : notification in-app, canal principal fonctionnel.
// Le push réel (FCM/WhatsApp/SMS, repli inter-canaux) est différé —
// nécessite une config Firebase absente pour l'instant (voir README).
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID destinataireActeurId;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String corps;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    private UUID referenceId; // ex: demandeId, missionId — selon le type

    @Builder.Default
    private Boolean lue = false;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum TypeNotification { PROPOSITION_RECUE, STATUT_MISSION, INFO_GENERALE }
}
