package com.flysoft.fretcorridor.ida.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// UC-IDA-03 : enrôlement assisté par agent de terrain, en ligne ou hors ligne
// (EF-IDA-06). RG-019 : le compte appartient à la personne enrôlée — l'OTP
// n'est jamais exposé à l'agent, seul le canal SMS le porte à la personne.
@Entity
@Table(name = "enrolements_agent")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrolementAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Acteur agent;

    @Column(nullable = false)
    private String telephoneEnrole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleActeur typeActeur;

    @Column(nullable = false)
    private String codeOtpHash;

    @Column(nullable = false)
    private LocalDateTime otpExpiration;

    // Position et horodatage de l'enrôlement (CDC UC-IDA-03, étape 5)
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private LocalDateTime dateEnrolement = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutEnrolement statut = StatutEnrolement.EN_ATTENTE;

    // Renseigné à l'activation — trace pour une future rémunération
    // conditionnée à l'activité réelle (RG-020, hors périmètre actuel).
    private UUID acteurCreeId;

    // Même logique que AuthService (MAX_TENTATIVES) : sans ce compteur, l'OTP
    // à 6 chiffres serait brute-forçable.
    @Builder.Default
    private Integer tentativesOtpEchouees = 0;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String tenantId;

    public enum StatutEnrolement {
        EN_ATTENTE,
        ACTIVE,
        EXPIRE
    }
}
