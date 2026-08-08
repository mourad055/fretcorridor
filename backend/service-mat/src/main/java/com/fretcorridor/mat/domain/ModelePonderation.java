package com.fretcorridor.mat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Version figee d'un ensemble de ponderations de matching (EF-MAT-04).
 *
 * Immuable une fois cree : changer les poids = creer une nouvelle version en
 * base (hors perimetre de cet increment - viendra avec un endpoint d'admin
 * dedie), jamais modifier une ligne existante. C'est ce qui garantit que
 * chaque CycleMatching passe reste reconstructible a l'identique (EF-MAT-11),
 * meme longtemps apres que les poids ont evolue.
 */
@Entity
@Table(name = "modele_ponderation", schema = "mat")
public class ModelePonderation {

    @Id
    @GeneratedValue
    private UUID id;

    // Nullable = modele par defaut (RG-106, meme principe que
    // opt.bareme_tarification.axeId) : utilise en repli si aucun modele
    // specifique a cet axe n'est actif.
    @Column(name = "axe_id")
    private UUID axeId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private boolean actif;

    @Column(length = 255)
    private String description;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected ModelePonderation() {
        // requis par JPA
    }

    public UUID getId() { return id; }
    public UUID getAxeId() { return axeId; }
    public Integer getVersion() { return version; }
    public boolean isActif() { return actif; }
    public String getDescription() { return description; }
    public Instant getDateCreation() { return dateCreation; }
}
