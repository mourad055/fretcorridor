package com.fretcorridor.adm.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "adm_dossier")
public class DossierEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.fretcorridor.adm.domain.TypeDossier type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.fretcorridor.adm.domain.PrioriteDossier priorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.fretcorridor.adm.domain.StatutDossier statut;

    private String missionId;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "adm_dossier_parties", joinColumns = @jakarta.persistence.JoinColumn(name = "dossier_id"))
    @Column(name = "partie")
    private List<String> parties;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "adm_dossier_preuves", joinColumns = @jakarta.persistence.JoinColumn(name = "dossier_id"))
    @Column(name = "preuve_reference")
    private List<String> preuvesReferences;

    @Column(nullable = false)
    private Instant ouvertLe;

    @Column(nullable = false)
    private Instant delaiTraitement;

    private String priseEnChargeParActeurId;
    private String decision;
    private String motifDecision;
    private String decidePar;
    private Instant decideLe;

    protected DossierEntity() {
    }

    public DossierEntity(String id, String tenantId, com.fretcorridor.adm.domain.TypeDossier type,
                          com.fretcorridor.adm.domain.PrioriteDossier priorite,
                          com.fretcorridor.adm.domain.StatutDossier statut, String missionId, List<String> parties,
                          List<String> preuvesReferences, Instant ouvertLe, Instant delaiTraitement,
                          String priseEnChargeParActeurId, String decision, String motifDecision, String decidePar,
                          Instant decideLe) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.priorite = priorite;
        this.statut = statut;
        this.missionId = missionId;
        this.parties = parties;
        this.preuvesReferences = preuvesReferences;
        this.ouvertLe = ouvertLe;
        this.delaiTraitement = delaiTraitement;
        this.priseEnChargeParActeurId = priseEnChargeParActeurId;
        this.decision = decision;
        this.motifDecision = motifDecision;
        this.decidePar = decidePar;
        this.decideLe = decideLe;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public com.fretcorridor.adm.domain.TypeDossier getType() {
        return type;
    }

    public com.fretcorridor.adm.domain.PrioriteDossier getPriorite() {
        return priorite;
    }

    public com.fretcorridor.adm.domain.StatutDossier getStatut() {
        return statut;
    }

    public String getMissionId() {
        return missionId;
    }

    public List<String> getParties() {
        return parties;
    }

    public List<String> getPreuvesReferences() {
        return preuvesReferences;
    }

    public Instant getOuvertLe() {
        return ouvertLe;
    }

    public Instant getDelaiTraitement() {
        return delaiTraitement;
    }

    public String getPriseEnChargeParActeurId() {
        return priseEnChargeParActeurId;
    }

    public String getDecision() {
        return decision;
    }

    public String getMotifDecision() {
        return motifDecision;
    }

    public String getDecidePar() {
        return decidePar;
    }

    public Instant getDecideLe() {
        return decideLe;
    }
}
