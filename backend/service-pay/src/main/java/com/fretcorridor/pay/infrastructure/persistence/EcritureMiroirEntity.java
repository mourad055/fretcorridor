package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ecritures_miroir")
public class EcritureMiroirEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "mission_id", nullable = false)
    private String missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_compte", nullable = false)
    private TypeCompte typeCompte;

    @Column(name = "beneficiaire_id")
    private String beneficiaireId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensEcriture sens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NatureEcriture nature;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "reference_prestataire")
    private String referencePrestataire;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEcriture statut;

    protected EcritureMiroirEntity() {
        // JPA
    }

    static EcritureMiroirEntity from(EcritureMiroir ecriture) {
        EcritureMiroirEntity entity = new EcritureMiroirEntity();
        entity.id = ecriture.id();
        entity.tenantId = ecriture.tenantId();
        entity.missionId = ecriture.missionId();
        entity.typeCompte = ecriture.typeCompte();
        entity.beneficiaireId = ecriture.beneficiaireId();
        entity.sens = ecriture.sens();
        entity.nature = ecriture.nature();
        entity.montant = ecriture.montant();
        entity.referencePrestataire = ecriture.referencePrestataire();
        entity.creeLe = ecriture.creeLe();
        entity.statut = ecriture.statut();
        return entity;
    }

    EcritureMiroir toDomain() {
        return new EcritureMiroir(id, tenantId, missionId, typeCompte, beneficiaireId, sens, nature, montant, referencePrestataire, creeLe, statut);
    }

    String getId() {
        return id;
    }

    void setStatut(StatutEcriture statut) {
        this.statut = statut;
    }
}
