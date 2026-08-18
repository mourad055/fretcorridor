package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.ModePaiement;
import com.fretcorridor.pay.domain.ModePaiementChoisi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "modes_paiement_choisis")
public class ModePaiementChoisiEntity {

    @Id
    @Column(name = "mission_id")
    private String missionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "choisi_le", nullable = false)
    private Instant choisiLe;

    protected ModePaiementChoisiEntity() {
        // JPA
    }

    static ModePaiementChoisiEntity from(ModePaiementChoisi choix) {
        ModePaiementChoisiEntity entity = new ModePaiementChoisiEntity();
        entity.missionId = choix.missionId();
        entity.tenantId = choix.tenantId();
        entity.modePaiement = choix.modePaiement();
        entity.choisiLe = choix.choisiLe();
        return entity;
    }

    ModePaiementChoisi toDomain() {
        return new ModePaiementChoisi(missionId, tenantId, modePaiement, choisiLe);
    }
}
