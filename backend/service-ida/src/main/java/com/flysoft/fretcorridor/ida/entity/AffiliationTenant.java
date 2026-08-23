package com.flysoft.fretcorridor.ida.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// S18 (Sprint 18, "Second tenant institutionnel", audit de suivi 23 aout) :
// rattachement d'un acteur (transporteur/chauffeur) a un tenant AUTRE que
// son tenant d'origine (Acteur.tenantId, inchange - reste l'identite KYC
// canonique). Cree uniquement par le second bureau qui invite/valide
// (regle produit choisie par l'utilisatrice - le bureau est la seule partie
// habilitee a accorder l'affiliation, jamais le transporteur lui-meme).
//
// Contrainte unique (acteur_id, tenant_id) : idempotence, un meme
// rattachement ne peut pas etre cree deux fois.
@Entity
@Table(name = "affiliations_tenant", uniqueConstraints = @UniqueConstraint(columnNames = {"acteur_id", "tenant_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliationTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "acteur_id", nullable = false)
    private UUID acteurId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
