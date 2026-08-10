package com.fretcorridor.bur.infrastructure.persistence;

import com.fretcorridor.bur.domain.MissionAppariee;
import com.fretcorridor.bur.domain.MissionAppparieeRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MissionAppparieeRepositoryAdapter implements MissionAppparieeRepositoryPort {

    private final MissionAppparieeJpaRepository jpaRepository;

    public MissionAppparieeRepositoryAdapter(MissionAppparieeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void enregistrer(MissionAppariee mission, UUID eventId) {
        jpaRepository.save(new MissionAppparieeEntity(
                eventId, mission.missionId(), mission.tenantId(), mission.axeId(), mission.transporteurId(),
                mission.origineNom(), mission.destinationNom(), mission.prixTransport(), mission.devise(),
                mission.confirmeeLe()));
    }

    @Override
    public List<MissionAppariee> listerParTenant(String tenantId) {
        return jpaRepository.findByTenantIdOrderByConfirmeeLeDesc(tenantId).stream()
                .map(e -> new MissionAppariee(
                        e.getMissionId(), e.getTenantId(), e.getAxeId(), e.getTransporteurId(),
                        e.getOrigineNom(), e.getDestinationNom(), e.getPrixTransport(), e.getDevise(),
                        e.getConfirmeeLe()))
                .toList();
    }
}
