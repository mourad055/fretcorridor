package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.LitigeMission;
import com.fretcorridor.pay.domain.LitigeMissionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class LitigeMissionRepositoryAdapter implements LitigeMissionPort {

    private final LitigeMissionJpaRepository jpaRepository;

    public LitigeMissionRepositoryAdapter(LitigeMissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<LitigeMission> parMission(String missionId) {
        return jpaRepository.findByMissionId(missionId).map(e -> new LitigeMission(
                e.getMissionId(), e.getTenantId(), e.isActif(), e.getHorodatage()));
    }

    @Override
    @Transactional
    public void enregistrerSiPlusRecent(LitigeMission litige) {
        jpaRepository.findByMissionId(litige.missionId())
                .ifPresentOrElse(
                        existant -> {
                            if (litige.horodatage().isAfter(existant.getHorodatage())) {
                                existant.mettreAJour(litige.actif(), litige.horodatage());
                            }
                            // sinon : message en retard/rejeu, ignoré silencieusement.
                        },
                        () -> jpaRepository.save(new LitigeMissionEntity(
                                litige.missionId(), litige.tenantId(), litige.actif(), litige.horodatage())));
    }
}
