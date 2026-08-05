package com.fretcorridor.adm.infrastructure.persistence;

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

    protected TenantEntity() {
    }

    public TenantEntity(String id, String nom, String pays) {
        this.id = id;
        this.nom = nom;
        this.pays = pays;
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
}
