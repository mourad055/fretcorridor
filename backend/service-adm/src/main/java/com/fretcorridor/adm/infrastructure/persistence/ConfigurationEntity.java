package com.fretcorridor.adm.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "adm_configuration")
public class ConfigurationEntity {

    @Id
    private String id;
    private String cle;
    private String perimetre;
    private String valeur;
    private String auteur;
    private int version;
    private Instant creeLe;

    protected ConfigurationEntity() {
    }

    public ConfigurationEntity(String id, String cle, String perimetre, String valeur, String auteur, int version,
                                Instant creeLe) {
        this.id = id;
        this.cle = cle;
        this.perimetre = perimetre;
        this.valeur = valeur;
        this.auteur = auteur;
        this.version = version;
        this.creeLe = creeLe;
    }

    public String getId() {
        return id;
    }

    public String getCle() {
        return cle;
    }

    public String getPerimetre() {
        return perimetre;
    }

    public String getValeur() {
        return valeur;
    }

    public String getAuteur() {
        return auteur;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreeLe() {
        return creeLe;
    }
}
