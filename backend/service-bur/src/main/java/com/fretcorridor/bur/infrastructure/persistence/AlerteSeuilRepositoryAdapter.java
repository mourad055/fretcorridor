package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.AlerteSeuil;
import com.fretcorridor.bur.domain.AlerteSeuilPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlerteSeuilRepositoryAdapter implements AlerteSeuilPort {

    private final AlerteSeuilJpaRepository jpaRepository;

    public AlerteSeuilRepositoryAdapter(AlerteSeuilJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void sauvegarder(AlerteSeuil alerte) {
        jpaRepository.save(new AlerteSeuilEntity(alerte.id(), alerte.tenantId(), alerte.axeId(), alerte.indicateur(),
                alerte.comparateur(), alerte.seuil(), alerte.creeParActeurId(), alerte.creeLe()));
    }

    @Override
    public List<AlerteSeuil> listerParTenant(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream().map(this::versDomaine).toList();
    }

    @Override
    public void supprimer(String id, String tenantId) {
        jpaRepository.deleteByIdAndTenantId(id, tenantId);
    }

    private AlerteSeuil versDomaine(AlerteSeuilEntity entity) {
        return new AlerteSeuil(entity.getId(), entity.getTenantId(), entity.getAxeId(), entity.getIndicateur(),
                entity.getComparateur(), entity.getSeuil(), entity.getCreeParActeurId(), entity.getCreeLe());
    }
}
