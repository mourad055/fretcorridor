package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.PositionRepositoryPort;
import com.fretcorridor.bur.domain.PositionVehicule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PositionRepositoryAdapter implements PositionRepositoryPort {

    private final PositionJpaRepository jpaRepository;

    public PositionRepositoryAdapter(PositionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void enregistrerSiPlusRecente(PositionVehicule position) {
        jpaRepository.findByMissionId(position.missionId())
                .ifPresentOrElse(
                        existante -> {
                            if (position.capturedLe().isAfter(existante.getCapturedLe())) {
                                existante.mettreAJour(position.vehiculeId(), position.latitude(),
                                        position.longitude(), position.capturedLe());
                            }
                            // sinon : message en retard/rejeu, ignoré silencieusement.
                        },
                        () -> jpaRepository.save(new PositionEntity(
                                position.missionId(), position.tenantId(), position.vehiculeId(),
                                position.latitude(), position.longitude(), position.capturedLe())));
    }

    @Override
    public List<PositionVehicule> listerParTenant(String tenantId) {
        return jpaRepository.findByTenantIdOrderByCapturedLeDesc(tenantId).stream()
                .map(e -> new PositionVehicule(
                        e.getMissionId(), e.getTenantId(), e.getVehiculeId(),
                        e.getLatitude(), e.getLongitude(), e.getCapturedLe()))
                .toList();
    }
}
