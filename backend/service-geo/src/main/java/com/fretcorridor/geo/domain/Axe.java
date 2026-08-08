package com.fretcorridor.geo.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Axe : liaison entre deux hubs (EF-GEO-01).
 *
 * Alimente en continu :
 *  - OPT (filtre L0) : seuls les axes avec matchingActif=true sont pris en compte
 *  - TRK (ecart de trajectoire) : referentiel origine/destination
 *
 * Les trois etats d'activation (EF-GEO-03) sont volontairement independants :
 * un axe peut etre visible cote mobile/web sans que le matching ou le paiement
 * y soient encore actives (ex. phase de lancement progressif d'un nouvel axe).
 */
@Entity
@Table(name = "axe", schema = "geo")
public class Axe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_origine_id", nullable = false)
    private Hub hubOrigine;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_destination_id", nullable = false)
    private Hub hubDestination;

    @Column(name = "visibilite_active", nullable = false)
    private boolean visibiliteActive = false;

    @Column(name = "matching_actif", nullable = false)
    private boolean matchingActif = false;

    @Column(name = "paiement_actif", nullable = false)
    private boolean paiementActif = false;

    // Parametres versionnes par axe (EF-GEO-02) : pondérations, rayon de matching, etc.
    // JSONB plutot que des colonnes dediees pour ne jamais coder en dur une regle metier
    // (anti-patron explicite du CDC §12.4).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parametres", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> parametres = Map.of();

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected Axe() {
        // Requis par JPA.
    }

    public Axe(String nom, Hub hubOrigine, Hub hubDestination) {
        this.nom = nom;
        this.hubOrigine = hubOrigine;
        this.hubDestination = hubDestination;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Hub getHubOrigine() {
        return hubOrigine;
    }

    public Hub getHubDestination() {
        return hubDestination;
    }

    public boolean isVisibiliteActive() {
        return visibiliteActive;
    }

    public boolean isMatchingActif() {
        return matchingActif;
    }

    public boolean isPaiementActif() {
        return paiementActif;
    }

    public Map<String, Object> getParametres() {
        return parametres;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    /** Active/desactive independamment un des trois etats (EF-GEO-03). */
    public void setVisibiliteActive(boolean visibiliteActive) {
        this.visibiliteActive = visibiliteActive;
    }

    public void setMatchingActif(boolean matchingActif) {
        this.matchingActif = matchingActif;
    }

    public void setPaiementActif(boolean paiementActif) {
        this.paiementActif = paiementActif;
    }

    public void setParametres(Map<String, Object> parametres) {
        this.parametres = parametres == null ? Map.of() : parametres;
    }
}
