package com.flysoft.fretcorridor.ida.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Entité centrale d'identité — cf. Plan d'exécution v4 §3 (modèle de données).
// Un acteur peut porter plusieurs rôles (ex: CHAUFFEUR_PROPRIETAIRE cumule
// chauffeur + transporteur) — relation n-n, pas un simple enum comme en v3.
@Entity
@Table(name = "acteurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acteur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String codePin; // hashé BCrypt

    @ElementCollection(targetClass = RoleActeur.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "acteur_roles", joinColumns = @JoinColumn(name = "acteur_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<RoleActeur> roles = new HashSet<>();

    // Nom/raison sociale — rempli à l'inscription légère (S1), complété au KYC (S2)
    private String nom;
    private String prenom;
    private String raisonSociale; // si personne morale (S2, sans Organisation dédiée pour l'instant simple)

    // S2 — lien vers l'entité Organisation si l'acteur est un collaborateur d'une personne morale
    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(nullable = false)
    private String tenantId; // "MARKETPLACE_CM" par défaut pour l'inscription publique chargeur

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NiveauKyc niveauKyc = NiveauKyc.NIVEAU_0;

    @Builder.Default
    private Boolean actif = true;

    @Builder.Default
    private Integer tentativesEchouees = 0;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    // RG-011 : KYC gradué à trois niveaux
    // NIVEAU_0 : téléphone vérifié, consultation seule
    // NIVEAU_1 : identité déclarée, publication de demande possible
    // NIVEAU_2 : pièces vérifiées, acceptation/paiement possibles (hors périmètre actuel)
    public enum NiveauKyc {
        NIVEAU_0,
        NIVEAU_1,
        NIVEAU_2
    }
}
