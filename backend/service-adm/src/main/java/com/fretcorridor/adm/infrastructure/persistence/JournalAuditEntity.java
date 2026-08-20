package com.fretcorridor.adm.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "adm_journal_audit")
public class JournalAuditEntity {

    @Id
    private String id;
    private String tenantId;
    private String acteurId;
    private String action;
    private String ressource;
    private Instant horodatage;

    protected JournalAuditEntity() {
    }

    public JournalAuditEntity(String id, String tenantId, String acteurId, String action, String ressource,
                               Instant horodatage) {
        this.id = id;
        this.tenantId = tenantId;
        this.acteurId = acteurId;
        this.action = action;
        this.ressource = ressource;
        this.horodatage = horodatage;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActeurId() {
        return acteurId;
    }

    public String getAction() {
        return action;
    }

    public String getRessource() {
        return ressource;
    }

    public Instant getHorodatage() {
        return horodatage;
    }
}
