package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.DeclarationEspeces;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "declarations_especes")
public class DeclarationEspecesEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "mission_id", nullable = false, unique = true)
    private String missionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "declaree_le", nullable = false)
    private Instant declareeLe;

    protected DeclarationEspecesEntity() {
        // JPA
    }

    static DeclarationEspecesEntity from(DeclarationEspeces declaration) {
        DeclarationEspecesEntity entity = new DeclarationEspecesEntity();
        entity.id = declaration.id();
        entity.tenantId = declaration.tenantId();
        entity.missionId = declaration.missionId();
        entity.montant = declaration.montant();
        entity.declareeLe = declaration.declareeLe();
        return entity;
    }

    DeclarationEspeces toDomain() {
        return new DeclarationEspeces(id, tenantId, missionId, montant, declareeLe);
    }
}
