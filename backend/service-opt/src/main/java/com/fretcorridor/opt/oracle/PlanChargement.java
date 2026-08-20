package com.fretcorridor.opt.oracle;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Oracle de chargement 3D (CDC S8.7, EF-MAT-07/13, Sprint 16, poste le plus
 * incertain du plan technique - "aucune bibliotheque libre ne le couvre
 * entierement").
 *
 * 1 PlanChargement par ETAT INTERMEDIAIRE (par EtapeTournee), jamais un seul
 * pour toute la tournee - EF-MAT-07 : "verification des charges a l'essieu a
 * CHAQUE etat intermediaire", un dechargement partiel en cours de route change
 * la repartition des charges restantes.
 *
 * S'applique UNIQUEMENT aux Tournee consolidees (LTL) - jamais a une
 * Affectation FTL simple (rien a verifier physiquement sur un lot unique deja
 * verifie par le profil vehicule seul). Meme distinction que
 * PropositionRetourAVideEvent (tourneeId/affectationId mutuellement exclusifs).
 *
 * ENF-DIS-04, mais avec une regle plus stricte que MAT/Valhalla (CDC UC-MAT-01,
 * flux d'exception E1) : si l'oracle ne peut pas conclure, la Tournee n'est
 * JAMAIS confirmee - pas de resultat degrade optimiste comme pour
 * CycleMatching/TarificationResultat. modeDegrade=true signale explicitement
 * ce blocage, jamais un echec silencieux.
 */
@Entity
@Table(name = "plan_chargement", schema = "opt")
public class PlanChargement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tournee_id", nullable = false)
    private UUID tourneeId;

    @Column(name = "etape_tournee_id", nullable = false)
    private UUID etapeTourneeId;

    // Cle = identifiant d'essieu (convention a definir avec ProfilCamionDto.
    // nombreEssieux), valeur = charge en tonnes a cet etat. JSONB pour ne
    // jamais coder en dur un nombre d'essieux fixe (anti-patron CDC S12.4).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "charges_par_essieu", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> chargesParEssieu = Map.of();

    // Structure ouverte tant que le contrat Lot/Colis (shared-contracts/
    // asyncapi/events/demande-publiee-lots.yaml, BROUILLON) n'est pas valide
    // avec Mobile - cf README_ORACLE_3D.md S6, point non tranche.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positions_colis", columnDefinition = "jsonb")
    private Map<String, Object> positionsColis;

    @Column(nullable = false)
    private boolean faisable;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "mode_degrade", nullable = false)
    private boolean modeDegrade = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    protected PlanChargement() {
        // Requis par JPA.
    }

    public PlanChargement(UUID tourneeId, UUID etapeTourneeId,
                           Map<String, Object> chargesParEssieu, Map<String, Object> positionsColis,
                           boolean faisable, String motifRejet, boolean modeDegrade) {
        this.tourneeId = tourneeId;
        this.etapeTourneeId = etapeTourneeId;
        this.chargesParEssieu = chargesParEssieu == null ? Map.of() : chargesParEssieu;
        this.positionsColis = positionsColis;
        this.faisable = faisable;
        this.motifRejet = motifRejet;
        this.modeDegrade = modeDegrade;
    }

    @PrePersist
    void onCreate() {
        this.dateCreation = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTourneeId() { return tourneeId; }
    public UUID getEtapeTourneeId() { return etapeTourneeId; }
    public Map<String, Object> getChargesParEssieu() { return chargesParEssieu; }
    public Map<String, Object> getPositionsColis() { return positionsColis; }
    public boolean isFaisable() { return faisable; }
    public String getMotifRejet() { return motifRejet; }
    public boolean isModeDegrade() { return modeDegrade; }
    public Instant getDateCreation() { return dateCreation; }
}
