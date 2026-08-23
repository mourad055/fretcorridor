package com.fretcorridor.adm.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "adm_tenant")
public class TenantEntity {

    @Id
    private String id;
    private String nom;
    private String pays;

    // default true en colonne (ddl-auto=update) : les tenants existants
    // avant ce champ restent actifs sans migration manuelle.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean actif;

    protected TenantEntity() {
    }

    public TenantEntity(String id, String nom, String pays, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.pays = pays;
        this.actif = actif;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPays() {
        return pays;
    }

    public boolean isActif() {
        return actif;
    }
}
