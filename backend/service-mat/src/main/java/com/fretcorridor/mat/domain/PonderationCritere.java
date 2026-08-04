package com.fretcorridor.mat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Poids d'un critere donne pour un modele de ponderation donne (EF-MAT-04).
 * code_critere reste une chaine libre (pas un enum Java) : ajouter un nouveau
 * critere de matching ne doit jamais necessiter de redeploiement du service,
 * juste une nouvelle ligne en base (anti-patron CDC S12.4, "aucun bareme code
 * en dur").
 */
@Entity
@Table(name = "ponderation_critere", schema = "mat")
public class PonderationCritere {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "modele_id", nullable = false)
    private UUID modeleId;

    @Column(name = "code_critere", nullable = false, length = 50)
    private String codeCritere;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal poids;

    protected PonderationCritere() {
        // requis par JPA
    }

    public UUID getId() { return id; }
    public UUID getModeleId() { return modeleId; }
    public String getCodeCritere() { return codeCritere; }
    public BigDecimal getPoids() { return poids; }
}
