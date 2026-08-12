package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.Sequestre;
import com.fretcorridor.pay.domain.SequestreEtat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sequestres")
public class SequestreEntity {

    @Id
    @Column(name = "mission_id")
    private String missionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SequestreEtat etat;

    @Column(name = "declenche_le")
    private Instant declencheLe;

    @Column(name = "libere_le")
    private Instant libereLe;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "transporteur_id")
    private String transporteurId;

    protected SequestreEntity() {
        // JPA
    }

    static SequestreEntity from(Sequestre sequestre) {
        SequestreEntity entity = new SequestreEntity();
        entity.missionId = sequestre.missionId();
        entity.etat = sequestre.etat();
        entity.declencheLe = sequestre.declencheLe();
        entity.libereLe = sequestre.libereLe();
        entity.tenantId = sequestre.tenantId();
        entity.transporteurId = sequestre.transporteurId();
        return entity;
    }

    Sequestre toDomain() {
        return new Sequestre(missionId, etat, declencheLe, libereLe, tenantId, transporteurId);
    }
}
