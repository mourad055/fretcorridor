package com.fretcorridor.opt.sequencement;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regroupe plusieurs Affectation (L1, deja confirmees) sur une MEME
 * capacite, dans un ordre donne (Sprint 11, CDC S8.6 - PDPTW/ALNS).
 *
 * Ne modifie jamais Affectation - cf sa javadoc ("immuable... toute
 * evolution future passera par une methode metier explicite"). Cette classe
 * EST cette methode metier : elle regroupe sans alterer.
 */
@Entity
@Table(name = "tournee", schema = "opt")
public class Tournee {

    public enum Statut { EN_CONSTRUCTION, CONFIRMEE, EN_EXECUTION, TERMINEE }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "capacite_id", nullable = false)
    private UUID capaciteId;

    @Column(name = "axe_id")
    private UUID axeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Statut statut = Statut.EN_CONSTRUCTION;

    @OneToMany(mappedBy = "tournee", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rang ASC")
    private List<EtapeTournee> etapes = new ArrayList<>();

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected Tournee() {
        // requis par JPA
    }

    public Tournee(UUID capaciteId, UUID axeId) {
        this.capaciteId = capaciteId;
        this.axeId = axeId;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    /**
     * EF-MAT-09 : toute tournee comportant au moins une etape EXECUTEE passe
     * en EN_EXECUTION - un ALNS ne doit plus jamais la recalculer
     * entierement, seules les etapes PLANIFIEE restent modifiables
     * (verifie par le solveur, pas ici).
     */
    public void marquerEnExecutionSiNecessaire() {
        boolean auMoinsUneEtapeExecutee = etapes.stream()
                .anyMatch(e -> e.getEtat() == EtapeTournee.Etat.EXECUTEE);
        if (auMoinsUneEtapeExecutee && statut == Statut.CONFIRMEE) {
            this.statut = Statut.EN_EXECUTION;
        }
        marquerTermineeSiToutesEtapesExecutees();
    }

    /**
     * EF-MAT-08/RG-058 (Sprint 12) : declenche l'eligibilite a une
     * proposition de retour a vide UNIQUEMENT quand TOUTES les etapes sont
     * EXECUTEE - jamais a la derniere livraison "probable", jamais en
     * anticipant (RG-058 : "aucune mission de retour n'est confirmee avant
     * la livraison effective de l'aller"). Une Tournee vide (aucune etape)
     * ne peut jamais devenir TERMINEE par cette voie - garde explicite,
     * cf etapes.isEmpty() ci-dessous.
     */
    private void marquerTermineeSiToutesEtapesExecutees() {
        if (statut != Statut.EN_EXECUTION) {
            return;
        }
        boolean toutesExecutees = !etapes.isEmpty()
                && etapes.stream().allMatch(e -> e.getEtat() == EtapeTournee.Etat.EXECUTEE);
        if (toutesExecutees) {
            this.statut = Statut.TERMINEE;
        }
    }

    public void confirmer() {
        if (statut != Statut.EN_CONSTRUCTION) {
            throw new IllegalStateException("Seule une tournee EN_CONSTRUCTION peut etre confirmee, statut actuel : " + statut);
        }
        this.statut = Statut.CONFIRMEE;
    }

    public UUID getId() { return id; }
    public UUID getCapaciteId() { return capaciteId; }
    public UUID getAxeId() { return axeId; }
    public Statut getStatut() { return statut; }
    public List<EtapeTournee> getEtapes() { return etapes; }
    public Instant getDateCreation() { return dateCreation; }
}
