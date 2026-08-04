package com.fretcorridor.opt.tarification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ligne de cout de service rattachee a un bareme (SOMME(COUT_SERVICES) du
 * S8.9.1 : manutention, attente, garde). code_service en chaine libre, meme
 * logique anti-bareme-en-dur que PonderationCritere.codeCritere cote
 * service-mat - ajouter un nouveau type de service ne doit jamais exiger de
 * redeploiement.
 *
 * LIMITE CONNUE DE CET INCREMENT (V0) : toutes les lignes rattachees a un
 * bareme actif sont sommees inconditionnellement (cf TarificationL4Service).
 * La selection conditionnelle (ex. "cout d'attente uniquement si depassement
 * de franchise") n'est pas modelisee ici - a traiter dans un increment
 * ulterieur si le besoin se confirme, plutot que de deviner une regle de
 * declenchement non specifiee par le CDC.
 */
@Entity
@Table(name = "composant_cout_service", schema = "opt")
public class ComposantCoutService {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "bareme_id", nullable = false)
    private UUID baremeId;

    @Column(name = "code_service", nullable = false, length = 50)
    private String codeService;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal montant;

    protected ComposantCoutService() {
        // requis par JPA
    }

    public UUID getId() { return id; }
    public UUID getBaremeId() { return baremeId; }
    public String getCodeService() { return codeService; }
    public BigDecimal getMontant() { return montant; }
}
